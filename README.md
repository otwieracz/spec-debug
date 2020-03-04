# spec-debug

Debug helper which I've been always missing - minimal helper for instrumenting `(clojure.spec.alpha/valid?)` with more verbose output.

This should be treated as **debug facility** and not be enabled on production by default - especially, when there are `spec/valid?` statements expected to fail non-catastrophically used in performace-sensitive parts of code.

## Usage

See if this sounds familiar:

```
;; Define some complex spec
(require '[clojure.spec.alpha :as spec])

(spec/def ::a string?)
(spec/def ::b string?)
(spec/def ::c pos-int?)
(spec/def ::my-complex-spec1 (spec/keys :req-un [::a ::b ::c]))
(spec/def ::d keyword?)
(spec/def ::my-even-more-complex-spec (spec/keys :req-un [::d ::my-complex-spec1]))

;; Now define a function with pre-condition
(defn my-fn
  [data]
  {:pre [(spec/valid? ::my-even-more-complex-spec data)]}
  (...))

;; And try calling it with data

user=> (my-fn {:d :foo :my-complex-spec1 {:a "foo" :b "bar" :c 123.0}})
Execution error (AssertionError) at user/my-fn (form-init9194193021643953961.clj:1).
Assert failed: (spec/valid? :user/my-even-more-complex-spec data)
class java.lang.AssertionError
    form-init9194193021643953961.clj:	1	user/my-fn
    core.clj:	3214	clojure.core/eval
    core.clj:	3210	clojure.core/eval
    main.clj:	414	clojure.main/repl/read-eval-print/fn
    main.clj:	435	clojure.main/repl/fn
    main.clj:	345	clojure.main/repl
user=> 

;; Lovely, but why it's failing?
```

In this case you probably end up changing every affected `spec/valid?` in `:pre` condition to `spec/explain`.

Or, alternatively, you can `(enable-spec-debug!)`.


```
user=> (require '[otwieracz.spec-debug :refer [enable-spec-debug! disable-spec-debug!]])
nil
user=> (enable-spec-debug!)
true
user=> (my-fn {:d :foo :my-complex-spec1 {:a "foo" :b "bar" :c 123.0}})
-- Spec failed --------------------

  {:d :foo,
   :my-complex-spec1 {:a "foo", :b "bar", :c 123.0}}
                                             ^^^^^

should satisfy

  pos-int?

-- Relevant specs -------

:user/c:
  clojure.core/pos-int?
:user/my-complex-spec1:
  (clojure.spec.alpha/keys :req-un [:user/a :user/b :user/c])
:user/my-even-more-complex-spec:
  (clojure.spec.alpha/keys :req-un [:user/d :user/my-complex-spec1])

-------------------------
Detected 1 error

Execution error (AssertionError) at user/my-fn (form-init9194193021643953961.clj:1).
Assert failed: (spec/valid? :user/my-even-more-complex-spec data)
class java.lang.AssertionError
    form-init9194193021643953961.clj:	1	user/my-fn
    core.clj:	3214	clojure.core/eval
    core.clj:	3210	clojure.core/eval
    main.clj:	414	clojure.main/repl/read-eval-print/fn
    main.clj:	435	clojure.main/repl/fn
    main.clj:	345	clojure.main/repl
```

## API
* `(otwieracz.spec-debug/enable-spec-debug! {:log-fn (...) :expound-options (...)})` - enable all instrumentation.
    * `log-fn`: function taking one argument, to be called with expound message represented as a string.
    * `expound-options`: map of `expound` printer options to be merged with `otwieracz.spec-debug/+expound-defaults+`. See https://cljdoc.org/d/expound/expound/0.8.4/doc/readme#printer-options for more details
* `(otwieracz.spec-debug/disable-spec-debug!)` - disable all instrumentation.

When debug is enabled, each failed `spec/valid?` call will trigger `expound` to explain it's cause and log it as error with `clojure.tools.logging`.

## Impact on performance
When altering multiple functions spanning over whole project, performance has to be taken into consideration.
At this moment, those "performance expectations" are covered by tests. In parenthesis there are sample values mesaured on Thinkpad x270 with Intel Core i5-7200U (while under load):
* performance impact less than 1.2x when spec verification succeed (sample 0.98x, managed to finish faster then standard code => no impact)
* performance overhead less than 5ms with default logger (sample 1.2ms)

Performance impact when reporting errors is not covered by tests. For the record, here are sample performance results:
* performance impact with default logger: sample 71x
* performance impact with no-op logger `(enable-spec-debug! {:log-fn nil})`: sample 42x

### Conclusion
* Performance impact is negligable when `spec/valid?` check succeeds.
* It has significant impact when check fails and result is being printed.
* `spec/valid?` failure in `:pre` and `:post` condition results in `AssertionError` and is not advised to be treated as "expected runtime error" and is usually leading to abnormal program termination - in such case any performance impact should not.

## Credits

Kudos to [github/bhb](https://github.com/bhb) for [expound](https://github.com/bhb/expound) library which made this little helper possible.

## License

Copyright © 2020 Slawomir Gonet

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
