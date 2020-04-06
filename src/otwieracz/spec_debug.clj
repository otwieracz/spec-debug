(ns otwieracz.spec-debug
  (:require [clojure.spec.alpha :as spec]
            [expound.alpha :as expound]
            [taoensso.timbre :as log]))

(def ^:private original-spec-valid? clojure.spec.alpha/valid?)

(def +expound-defaults+ {:show-valid-values? true})

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
                              (fn [message] (str "clojure.spec.alpha validation error:\n"
                                                 (log/error message))))]
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