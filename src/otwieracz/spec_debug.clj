(ns otwieracz.spec-debug
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [taoensso.timbre :as log]))

(def ^:private original-spec-valid? clojure.spec.alpha/valid?)

(def +expound-defaults+ {:show-valid-values? true})

(defn- filter-stack-trace
  "Filter non-clojure and own (otwieracz.spec_debug) stacktrace frames"
  [stack-trace]
  (->> stack-trace
       ;; keep only clj{,c,x,s} frames
       (filter (fn [frame]
                 (re-matches #".*\.clj[csx]{0,1}$" (.getFileName frame))))
       ;; delete all expound frames
       (remove (fn [frame]
                 (re-matches #"^expound.*" (.getClassName frame))))
       ;; delete all otwieracz.spec_debug frames and files
       (remove (fn [frame]
                 (re-matches #"^otwieracz\.spec[-_]debug.*" (.getClassName frame))))
       (remove (fn [frame]
                 (re-matches #"^otwieracz\.spec[-_]debug.*" (.getFileName frame))))))

(defn- wrap-expound-log-error
  ([options valid?-fn]
   (fn [& args]
     (or (apply valid?-fn args)
         (try
           ;; we could easily use three-argument `explain-str` here, but
           ;; it's only present in most recent verisons of `expound` library.
           ;; defining custom printer gives us a bit of backward compatibility
           ;;
           ;; in worst case exception will be thrown and catched without any
           ;; side effects
           (let [printer (expound/custom-printer +expound-defaults+)
                 explain-data (spec/explain-data (first args) (second args))
                 message (with-out-str (printer explain-data))
                 log-fn  (get options
                              :log-fn
                              (fn [message]
                                ;; get original file and line
                                (let [stack-trace (-> (Throwable.) .fillInStackTrace .getStackTrace
                                                      filter-stack-trace)
                                      frame (first stack-trace)]
                                  (log/log! :error
                                            :p
                                            [(str (.getClassName frame) ": clojure.spec.alpha/valid? failed:\n" message)]
                                            {:?file (.getFileName frame)
                                           ;; use file-name as ns-str to print something like:
                                           ;; 20-04-06 10:32:41 odyssey INFO [foobar.clj:1234] - foo
                                             :?ns-str (.getFileName frame)
                                             :?line (.getLineNumber frame)}))))]
             (when log-fn
               (log-fn message))
             false)
           ;; Catch every Throwable just in case to make no excpetion is thrown
           ;; from the wrapping function
           (catch Throwable _ false))))))

(defn enable-spec-debug!
  "Globally enable debugging of `spec/valid?`. Optionally provide `options` for handler."
  ([options]
   (alter-var-root (var clojure.spec.alpha/valid?) (partial wrap-expound-log-error options))
   true)
  ([]
   (enable-spec-debug! nil)))

(defn disable-spec-debug!
  "Globally disable debugging of `spec/valid?`."
  []
  (alter-var-root (var clojure.spec.alpha/valid?) (constantly original-spec-valid?))
  false)

(comment
  (defn test []
    (enable-spec-debug!)
    (spec/valid? string? 123)
    (disable-spec-debug!))
  (test))
