;
; Copyright © 2017 Colin Smith.
; This work is based on the Scmutils system of MIT/GNU Scheme:
; Copyright © 2002 Massachusetts Institute of Technology
;
; This is free software;  you can redistribute it and/or modify
; it under the terms of the GNU General Public License as published by
; the Free Software Foundation; either version 3 of the License, or (at
; your option) any later version.
;
; This software is distributed in the hope that it will be useful, but
; WITHOUT ANY WARRANTY; without even the implied warranty of
; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
; General Public License for more details.
;
; You should have received a copy of the GNU General Public License
; along with this code; if not, see <http://www.gnu.org/licenses/>.
;

(ns sicmutils.function-test
  (:require [clojure.test :refer [is deftest testing]]
            [clojure.test.check.generators :as gen]
            [com.gfredericks.test.chuck.clojure-test :refer [checking]
             #?@(:cljs [:include-macros true])]
            [same :refer [ish? with-comparator]
             #?@(:cljs [:include-macros true])]
            [sicmutils.function :as f]
            [sicmutils.generators :as sg]
            [sicmutils.generic :as g]
            [sicmutils.numbers]
            [sicmutils.value :as v]))

(deftest value-protocol-tests
  (testing "v/zero? returns false for fns"
    (is (not (v/zero? neg?)))
    (is (not (v/zero? #'neg?)))
    (is (not (v/zero? g/add))))

  (testing "v/one? returns false for fns"
    (is (not (v/one? neg?)))
    (is (not (v/one? #'neg?)))
    (is (not (v/one? g/add)))
    (is (not (v/one? identity))))

  (testing "v/identity? returns false for fns"
    (is (not (v/identity? neg?)))
    (is (not (v/identity? #'neg?)))
    (is (not (v/identity? g/add)))
    (is (not (v/identity? identity))
        "We go conservative and say that EVEN the actual identity function is not identity."))

  (testing "v/numerical? returns false for fns"
    (is (not (v/numerical? neg?)))
    (is (not (v/numerical? #'neg?)))
    (is (not (v/numerical? g/add)))
    (is (not (v/numerical? identity))))

  (checking "zero-like, one-like returns 0, 1 for fns, vars" 100
            [f (gen/elements [g/negative? g/abs g/sin g/cos
                              #'g/negative? #'g/abs #'g/sin #'g/cos])
             n sg/real]
            (is (== 0 ((v/zero-like f) n)))
            (is (== 1 ((v/one-like f) n))))

  (checking "identity-like returns the identity fn" 100
            [f (gen/elements [g/negative? g/abs g/sin g/cos
                              #'g/negative? #'g/abs #'g/sin #'g/cos])
             n sg/real]
            (is (= n ((v/identity-like f) n))))

  (checking "exact? mirrors input" 100 [n sg/real]
            (if (v/exact? n)
              (is ((v/exact? identity) n))
              (is (not ((v/exact? identity) n)))))

  (testing "v/freeze"
    (is (= ['+ '- '* '/ 'modulo 'quotient 'remainder 'negative?]
           (map v/freeze [+ - * / mod quot rem neg?]))
        "Certain functions freeze to symbols")

    (is (= (map v/freeze [g/+ g/- g/* g//
                          g/modulo g/quotient g/remainder
                          g/negative?])
           (map v/freeze [+ - * / mod quot rem neg?]))
        "These freeze to the same symbols as their generic counterparts.")

    (let [f (fn [x] (* x x))]
      (is (= f (v/freeze f))
          "Unknown functions freeze to themselves")))

  (testing "v/kind returns ::v/function"
    (is (= ::v/function (v/kind neg?)))
    (is (= ::v/function (v/kind #'neg?)))
    (is (= ::v/function (v/kind g/add)))
    (is (= ::v/function (v/kind (fn [x] (* x x)))))))

#?(:cljs
   (deftest exposed-arities-test
     (is (= [1] (f/exposed-arities (fn [x] (* x x)))))
     (is (= [1 3] (f/exposed-arities (fn ([x] (* x x)) ([x y z] (+ x y))))))))

(deftest arities
  (is (= [:exactly 0] (f/arity (fn [] 42))))
  (is (= [:exactly 1] (f/arity (fn [x] (+ x 1)))))
  (is (= [:exactly 2] (f/arity (fn [x y] (+ x y)))))
  (is (= [:exactly 3] (f/arity (fn [x y z] (* x y z)))))
  (is (= [:at-least 0] (f/arity (fn [& xs] (reduce + 0 xs)))))
  (is (= [:at-least 1] (f/arity (fn [x & xs] (+ x (reduce * 1 xs))))))
  (is (= [:at-least 2] (f/arity (fn [x y & zs] (+ x y (reduce * 1 zs))))))
  (is (= [:exactly 0] (f/arity 'x)))
  (is (= [:at-least 0] (f/arity (constantly 42))))
  ;; the following is dubious until we attach arity metadata to MultiFns
  (is (= [:exactly 1] (f/arity [1 2 3])))
  (let [f (fn [x] (+ x x))
        g (fn [y] (* y y))]
    (is (= [:exactly 1] (f/arity (comp f g))))))

#?(:cljs
   (deftest arities-cljs
     ;; in cljs, we can figure out that a fn accepts some bounded number of
     ;; arguments.
     (is (= [:between 1 3] (f/arity (fn ([x] (inc x))
                                      ([x y] (+ x y))
                                      ([x y z] (* x y z))))))

     ;; Noting the case here where we're missing the arity-2, but we still
     ;; return a :between.
     (is (= [:between 1 3] (f/arity (fn ([x] (inc x)) ([x y z] (* x y z))))))

     ;; Adding a variadic triggers :at-least...
     (is (= [:at-least 1] (f/arity (fn ([x] (inc x)) ([x y z & xs] (* x y z))))))

     ;; Unless you add ALL arities from 0 to 3 and variadic. Then we assume you were
     ;; generated by comp.
     (is (= [:exactly 1] (f/arity (fn ([] 10) ([x] (inc x)) ([x y]) ([x y z & xs] (* x y z))))))

     ;; A single variadic with lots of args works too.
     (is (= [:at-least 4] (f/arity (fn [x y z a & xs] (* x y z a)))))))

(defn illegal? [f]
  (is (thrown? #?(:clj IllegalArgumentException :cljs js/Error)
               (f))))

(deftest joint-arities
  (is (= [:exactly 1] (f/joint-arity [[:exactly 1] [:exactly 1]])))
  (is (= [:exactly 5] (f/joint-arity [[:exactly 5] [:exactly 5]])))
  (is (illegal? #(f/joint-arity [[:exactly 2] [:exactly 1]])))
  (is (illegal? #(f/joint-arity [[:exactly 1] [:exactly 2]])))
  (is (= [:exactly 3] (f/joint-arity [[:exactly 3] [:at-least 2]])))
  (is (= [:exactly 3] (f/joint-arity [[:exactly 3] [:at-least 3]])))
  (is (= [:exactly 3] (f/joint-arity [[:at-least 1] [:exactly 3]])))
  (is (= [:exactly 3] (f/joint-arity [[:at-least 3] [:exactly 3]])))
  (is (illegal? #(f/joint-arity [[:exactly 1] [:at-least 2]])))
  (is (illegal? #(f/joint-arity [[:at-least 2] [:exactly 1]])))
  (is (= [:at-least 3] (f/joint-arity [[:at-least 2] [:at-least 3]])))
  (is (= [:at-least 3] (f/joint-arity [[:at-least 3] [:at-least 2]])))
  (is (= [:between 2 3] (f/joint-arity [[:between 1 3] [:between 2 5]])))
  (is (= [:between 2 3] (f/joint-arity [[:between 2 5] [:between 1 3]])))
  (is (illegal? #(f/joint-arity [[:between 1 3] [:between 4 6]])))
  (is (illegal? #(f/joint-arity [[:between 4 6] [:between 1 3]])))
  (is (= [:exactly 3] (f/joint-arity [[:between 1 3] [:between 3 4]])))
  (is (= [:exactly 3] (f/joint-arity [[:between 3 4] [:between 1 3]])))
  (is (= [:between 2 4] (f/joint-arity [[:at-least 2] [:between 1 4]])))
  (is (= [:between 2 4] (f/joint-arity [[:between 1 4] [:at-least 2]])))
  (is (illegal? #(f/joint-arity [[:at-least 4] [:between 1 3]])))
  (is (illegal? #(f/joint-arity [[:between 1 3] [:at-least 4]])))
  (is (= [:exactly 2] (f/joint-arity [[:exactly 2] [:between 2 3]])))
  (is (= [:exactly 2] (f/joint-arity [[:between 2 3] [:exactly 2]])))
  (is (illegal? #(f/joint-arity [[:between 2 3] [:exactly 1]])))
  (is (illegal? #(f/joint-arity [[:exactly 1] [:between 2 3]]))))

(deftest trig-tests
  (with-comparator (v/within 1e-8)
    (checking "tan, sin, cos" 100
              [n sg/real]
              (let [f (g/- g/tan (g/div g/sin g/cos))]
                (is (ish? 0 (f n)))))

    (checking "cos/acos" 100 [n (sg/reasonable-double {:min -100 :max 100})]
              (let [f (f/compose g/cos g/acos)]
                (is (ish? n (f n)))))

    (checking "sin/asin" 100 [n (sg/reasonable-double {:min -100 :max 100})]
              (let [f (f/compose g/sin g/asin)]
                (is (ish? n (f n)))))

    (checking "tan/atan" 100 [n (sg/reasonable-double {:min -100 :max 100})]
              (let [f (f/compose g/tan g/atan)]
                (is (ish? n (f n))))))

  (testing "tan/atan, 2 arity version"
    (let [f (f/compose g/tan g/atan)]
      (is (ish? (/ 0.5 0.2) (f 0.5 0.2)))))

  (with-comparator (v/within 1e-10)
    (checking "tan, sin, cos" 100 [n sg/real]
              (let [f (g/- g/tan (g/div g/sin g/cos))]
                (when-not (v/zero? n)
                  (is (ish? 0 (f n))))))

    (checking "cot" 100 [n sg/real]
              (let [f (g/- g/cot (g/invert g/tan))]
                (when-not (v/zero? n)
                  (is (ish? 0 (f n)))))))

  (checking "tanh" 100 [n (sg/reasonable-double {:min -100 :max 100})]
            (let [f (g/- (g/div g/sinh g/cosh) g/tanh)]
              (is (ish? 0 (f n)))))

  (checking "sec" 100 [n sg/real]
            (let [f (g/- (g/invert g/cos) g/sec)]
              (is (ish? 0 (f n)))))

  (checking "csc" 100 [n sg/real]
            (let [f (g/- (g/invert g/sin) g/csc)]
              (when-not (v/zero? n)
                (is (ish? 0 (f n))))))

  (checking "sech" 100 [n sg/real]
            (let [f (g/- (g/invert g/cosh) g/sech)]
              (is (ish? 0  (f n)))))

  (checking "cosh" 100 [n sg/real]
            (is (ish? ((g/cosh g/square) n)
                      (g/cosh (g/square n)))))

  (checking "sinh" 100 [n sg/real]
            (is (ish? ((g/sinh g/square) n)
                      (g/sinh (g/square n)))))

  (with-comparator (v/within 1e-8)
    (checking "acosh" 100
              [n (sg/reasonable-double {:min -100 :max 100})]
              (let [f (f/compose g/cosh g/acosh)]
                (is (ish? n (f n)))))

    (checking "asinh" 100
              [n (sg/reasonable-double {:min -100 :max 100})]
              (let [f (f/compose g/sinh g/asinh)]
                (is (ish? n (f n)))))

    (checking "atanh" 100
              [n (sg/reasonable-double {:min -10 :max 10})]
              (when-not (v/one? (g/abs n))
                (let [f (f/compose g/tanh g/atanh)]
                  (is (ish? n (f n))))))))

(deftest complex-tests
  (testing "gcd/lcm unit"
    (is (= (g/gcd 10 5)
           (let [deferred (g/gcd (g/+ 1 g/square) 5)]
             (deferred 3))))
    (is (= (g/lcm 10 6)
           (let [deferred (g/lcm (g/+ 1 g/square) 6)]
             (deferred 3))))))

(deftest transpose-test
  (testing "transpose"
    (let [f #(str "f" %)
          g #(str "g" %)]
      (is (= "fg" (f (g ""))))
      (is (= "gf" (((g/transpose f) g) ""))
          "g/transpose for functions returns a fn that takes ANOTHER fn, and
    returns a fn that applies them in reverse order. Like a curried andThen (the
    reverse of compose)"))))

(deftest function-algebra
  (let [add2 (fn [x] (g/+ x 2))
        explog (g/exp g/log)
        mul3 #(* 3 %)]
    (testing "unary"
      (is (= 4 (add2 2)))
      (is (= -4 ((g/- add2) 2)))
      (is (= 9 ((g/sqrt add2) 79)))
      (is (= #sicm/ratio 1/9 ((g/invert add2) 7)))
      (is (= 1.0 (explog 1.0)))
      (is (ish? 99.0 (explog 99.0)))
      (is (ish? 20.085536923187668 ((g/exp add2) 1.0)))
      (is (ish? 4.718281828459045 ((add2 g/exp) 1.0))))

    (testing "binary"
      (is (= 12 ((g/+ add2 4) 6)))
      (is (= 14 ((g/+ add2 mul3) 3)))
      (is (= 10 ((g/+ mul3 4) 2)))
      (is (= 32 ((g/expt 2 add2) 3)))
      (is (= 25 ((g/expt add2 2) 3)))
      (is (= ::v/function (v/kind (g/expt add2 2)))))

    (testing "determinant"
      (is (= 20 ((g/determinant *) 4 5))))

    (checking "invert" 100 [n sg/real]
              (when-not (v/zero? n)
                (is (= ((g/+ 1 g/invert) n)
                       (g/+ 1 (g/invert n))))))

    (checking "negative?" 100 [n sg/real]
              (is (not ((g/negative? g/abs) n)))
              (when-not (v/zero? n)
                (is ((g/negative? (f/compose g/negate g/abs)) n))))

    (checking "abs" 100 [n sg/real]
              (is (= ((g/+ 1 g/abs) n)
                     (g/+ 1 (g/abs n)))))

    (checking "quotient" 100 [l sg/any-integral
                              r sg/native-integral]
              (when-not (v/zero? r)
                (is (= ((g/+ 1 g/quotient) l r)
                       (g/+ 1 (g/quotient l r))))))

    (checking "exact-divide" 100 [n (gen/choose -200 200)
                                  m (gen/choose -20 20)]
              (when-not (v/zero? m)
                (is (= n ((g/exact-divide g/* m) n m))
                    "The f position here is a function that takes 2 elements,
                  passes them to g/*, the calls exact-divide on the result and
                  `m`.")))

    (checking "dimension" 100 [n sg/real]
              (is (= ((g/invert g/dimension) n)
                     (g/dimension n))
                  "The dimension of a number is always 1."))

    (checking "remainder" 100 [l sg/any-integral
                               r sg/native-integral]
              (when-not (v/zero? r)
                (is (= ((g/+ 1 g/remainder) l r)
                       (g/+ 1 (g/remainder l r))))))

    (checking "modulo" 100 [l sg/any-integral
                            r sg/native-integral]
              (when-not (v/zero? r)
                (is (= ((g/+ 1 g/modulo) l r)
                       (g/+ 1 (g/modulo l r))))))

    (testing "arity 2"
      (let [f (fn [x y] (+ x y))
            g (fn [x y] (* x y))
            h (g/+ f g)
            k (g/+ 4 (g/- f 2))
            m (g/+ g (g/- f 2))]
        (is (= 11 (h 2 3)))
        (is (= 7 (k 2 3)))
        (is (= 9 (m 2 3)))))

    (testing "arity 0"
      (let [f (fn [] 3)
            g (fn [] 4)
            h (g/+ f g)
            k (g/- f g)
            j (g/* f g)
            q (g/divide f g)]
        (is (= 7 (h)))
        (is (= -1 (k)))
        (is (= 12 (j)))
        (is (= #sicm/ratio 3/4 (q)))))

    (testing "at least 0 arity"
      (let [add (fn [& xs] (reduce + 0 xs))
            mul (fn [& xs] (reduce * 1 xs))
            add+mul (g/+ add mul)
            add-mul (g/- add mul)
            mul-add (g/- mul add)]
        (is (= [:at-least 0] (f/arity add)))
        (is (= [:at-least 0] (f/arity mul)))
        (is (= [:at-least 0] (f/arity add+mul)))
        (is (= 33 (add+mul 2 3 4)))
        (is (= -15 (add-mul 2 3 4)))
        (is (= 15 (mul-add 2 3 4)))))))
