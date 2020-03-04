(defproject otwieracz/spec-debug "0.1.0"
  :description "Minimal helper for instrumenting `(clojure.spec.alpha/valid?) `with more verbose output."
  :url "https://github.com/otwieracz/spec-debug"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.logging "1.0.0"]
                 [expound "0.8.4"]]
  :repl-options {:init-ns otwieracz.spec-debug})