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

(ns sicmutils.matrix
  (:refer-clojure :rename {get-in core-get-in}
                  #?@(:cljs [:exclude [get-in]]))
  (:require [sicmutils.value :as v]
            [sicmutils.expression :as x]
            [sicmutils.function :as f]
            [sicmutils.generic :as g]
            [sicmutils.util :as u]
            [sicmutils.util.aggregate :as ua]
            [sicmutils.structure :as s]
            [sicmutils.series :as series])
  #?(:clj
     (:import [clojure.lang AFn Counted IFn ILookup Seqable Sequential])))

(declare fmap generate I)

(derive ::square-matrix ::matrix)
(derive ::column-matrix ::matrix)
(derive ::row-matrix ::matrix)
(derive ::matrix ::f/cofunction)

(deftype Matrix [r c v]
  v/Value
  (zero? [_] (every? #(every? v/zero? %) v))
  (one? [_] false)
  (identity? [_] false)
  (zero-like [this] (fmap v/zero-like this))
  ;; TODO: one-like/identity-like should use a recursive copy to find the 1 elements
  (one-like [_]
    (if-not (= r c)
      (u/illegal "one-like on non-square")
      (I r)))
  (identity-like [_]
    (if-not (= r c)
      (u/illegal "identity-like on non-square")
      (I r)))
  (freeze [_] (if (= c 1)
                `(~'column-matrix ~@(map (comp v/freeze first) v))
                `(~'matrix-by-rows ~@(map v/freeze v))))
  (numerical? [_] false)
  (exact? [_] (every? #(every? v/exact? %) v))
  (kind [_] (cond (= r c) ::square-matrix
                  (= r 1) ::row-matrix
                  (= c 1) ::column-matrix
                  :else ::matrix))

  #?@(:clj
      [Object
       (equals [_ b]
               (and (instance? Matrix b)
                    (let [^Matrix bm b]
                      (and (= r (.-r bm))
                           (= c (.-c bm))
                           (= v (.-v bm))))))
       (toString [_] (str v))

       Sequential

       Counted
       (count [_] (count v))

       Seqable
       (seq [_] (seq v))

       ILookup
       (valAt [_ key] (get v key))
       (valAt [_ key default] (get v key default))

       IFn
       (invoke [_ a]
               (Matrix. r c (mapv (fn [e] (mapv #(% a) e)) v)))
       (invoke [_ a b]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b) e)) v)))
       (invoke [_ a b c]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c) e)) v)))
       (invoke [_ a b c d]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d) e)) v)))
       (invoke [_ a b c d e]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e) e)) v)))
       (invoke [_ a b c d e f]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f) e)) v)))
       (invoke [_ a b c d e f g]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g) e)) v)))
       (invoke [_ a b c d e f g h]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h) e)) v)))
       (invoke [_ a b c d e f g h i]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i) e)) v)))
       (invoke [_ a b c d e f g h i j]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j) e)) v)))
       (invoke [_ a b c d e f g h i j k]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k) e)) v)))
       (invoke [_ a b c d e f g h i j k l]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l) e)) v)))
       (invoke [_ a b c d e f g h i j k l m]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p q]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p q r]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p q r s]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r s) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p q r s t]
               (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r s t) e)) v)))
       (invoke [_ a b c d e f g h i j k l m n o p q r s t rest]
               (Matrix. r c (mapv (fn [e] (mapv #(apply % a b c d e f g h i j k l m n o p q r s t rest) e)) v)))
       (applyTo [m xs]
                (AFn/applyToHelper m xs))]

      :cljs
      [IEquiv
       (-equiv [_ b]
               (if (instance? Matrix b)
                 (and (= r (.-r b))
                      (= c (.-c b))
                      (= v (.-v b)))
                 (= v (seq b))))

       Object
       (toString [_] (str v))

       IPrintWithWriter
       (-pr-writer [x writer _]
                   (write-all writer
                              "#object[sicmutils.structure.Matrix \""
                              (.toString x)
                              "\"]"))

       ISequential

       ICounted
       (-count [_] (-count v))

       ISeqable
       (-seq [_] (-seq v))

       ILookup
       (-lookup [_ k] (-lookup v k))
       (-lookup [_ k default] (-lookup v k default))

       IFn
       (-invoke [_ a]
                (Matrix. r c (mapv (fn [e] (mapv #(% a) e)) v)))
       (-invoke [_ a b]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b) e)) v)))
       (-invoke [_ a b c]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c) e)) v)))
       (-invoke [_ a b c d]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d) e)) v)))
       (-invoke [_ a b c d e]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e) e)) v)))
       (-invoke [_ a b c d e f]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f) e)) v)))
       (-invoke [_ a b c d e f g]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g) e)) v)))
       (-invoke [_ a b c d e f g h]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h) e)) v)))
       (-invoke [_ a b c d e f g h i]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i) e)) v)))
       (-invoke [_ a b c d e f g h i j]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j) e)) v)))
       (-invoke [_ a b c d e f g h i j k]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k) e)) v)))
       (-invoke [_ a b c d e f g h i j k l]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p q]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p q r]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p q r s]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r s) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p q r s t]
                (Matrix. r c (mapv (fn [e] (mapv #(% a b c d e f g h i j k l m n o p q r s t) e)) v)))
       (-invoke [_ a b c d e f g h i j k l m n o p q r s t rest]
                (Matrix. r c (mapv (fn [e] (mapv #(apply % a b c d e f g h i j k l m n o p q r s t rest) e)) v)))]))

(defn matrix?
  [m]
  (instance? Matrix m))

(defn square?
  "Returns true if `m` is a square matrix, false otherwise."
  [m]
  (and (matrix? m)
       (= (.-r ^Matrix m)
          (.-c ^Matrix m))))

(defn column?
  "Returns true if `m` is a matrix with a single column (a 'column matrix'),
  false otherwise."
  [m]
  (and (matrix? m)
       (= (.-c ^Matrix m) 1)))

(defn row?
  "Returns true if `m` is a matrix with a single row (a 'row matrix'), false
  otherwise."
  [m]
  (and (matrix? m)
       (= (.-r ^Matrix m) 1)))

(defn generate
  "Create the r by c matrix whose entries are (f i j)"
  [r c f]
  (->Matrix r c
            (mapv (fn [i]
                    (mapv (fn [j]
                            (f i j))
                          (range c)))
                  (range r))))

(defn literal-matrix
  "Generates a `nrows` x `ncols` matrix of symbolic entries, each prefixed by the
  supplied symbol `sym`.

  For example:

  (= (literal-matrix 'x 2 2)
     (by-rows ['x↑0_0 'x↑0_1]
              ['x↑1_0 'x↑1_1]))"
  [sym nrows ncols]
  (let [prefix (str sym "↑")]
    (generate nrows ncols
              (fn [i j]
                (symbol (str prefix i "_" j))))))

(defn get-in
  "Like get-in for matrices, but obeying the scmutils convention: only one
  index is required to get an unboxed element from a column vector. This is
  perhaps an unprincipled exception..."
  [^Matrix m is]
  (let [e (core-get-in (.-v m) is)]
    (if (and (= 1 (count is))
             (= 1 (.-c m)))
      (e 0)
      e)))

(defn matrix-some
  "True if f is true for some element of m."
  [f ^Matrix m]
  (some f (flatten (.-v m))))

(defn fmap
  "Maps f over the elements of m, returning an object of the same type."
  [f ^Matrix m]
  (->Matrix (.-r m) (.-c m) (mapv #(mapv f %) (.-v m))))

(defn by-rows [& rs]
  {:pre [(seq rs)
         (every? seq rs)]}
  (let [r (count rs)
        cs (map count rs)]
    (when-not (every? #(= % (first cs)) (next cs))
      (u/illegal "malformed matrix"))
    (->Matrix r (first cs) (mapv vec rs))))

(defn column [& es]
  {:pre [(not-empty es)]}
  (->Matrix (count es) 1 (vec (for [e es] [e]))))

(defn transpose
  "Transpose the matrix m."
  [^Matrix m]
  (let [v (.-v m)]
    (generate (.-c m) (.-r m) #(core-get-in v [%2 %1]))))

(defn ->structure
  "Convert m to a structure with given outer and inner orientations. Rows of
  M will become the inner tuples, unless t? is true, in which columns of m will
  form the inner tuples."
  [m outer-orientation inner-orientation t?]
  (let [^Matrix m' (if t? (transpose m) m)
        v (.-v m')]
    (s/->Structure outer-orientation
                   (mapv #(s/->Structure inner-orientation %) v))))

(defn seq->
  "Convert a sequence (typically, of function arguments) to an up-structure.
  GJS: Any matrix in the argument list wants to be converted to a row of
  columns"
  [s]
  (apply s/up (map #(if (instance? Matrix %) (->structure % s/down s/up false) %) s)))

(defn ^:private mul
  "Multiplies the two matrices a and b"
  [^Matrix a ^Matrix b]
  (let [ra (.-r a)
        rb (.-r b)
        ca (.-c a)
        cb (.-c b)
        va (.-v a)
        vb (.-v b)]
    (when (not= ca rb)
      (u/illegal "matrices incompatible for multiplication"))
    (generate ra cb #(reduce g/+ (for [k (range ca)]
                                   (g/* (core-get-in va [%1 k])
                                        (core-get-in vb [k %2])))))))

(defn ^:private elementwise
  "Applies f elementwise between the matrices a and b."
  [f ^Matrix a ^Matrix b]
  (let [ra (.-r a)
        rb (.-r b)
        ca (.-c a)
        cb (.-c b)
        va (.-v a)
        vb (.-v b)]
    (when (or (not= ra rb) (not= ca cb))
      (u/illegal "matrices incompatible for operation"))
    (generate ra ca #(f (core-get-in va [%1 %2]) (core-get-in vb [%1 %2])))))

(defn square-structure->
  "Converts the square structure s into a matrix, and calls the
  continuation with that matrix and a function which will restore a
  matrix to a structure with the same inner and outer orientations as
  s."
  [s k]
  (let [major-size (count s)
        major-orientation (s/orientation s)
        minor-sizes (map #(if (s/structure? %) (count %) 1) s)
        minor-orientations (map s/orientation s)
        minor-orientation (first minor-orientations)]
    (if (and (every? #(= major-size %) minor-sizes)
             (every? #(= minor-orientation %) (rest minor-orientations)))
      (let [need-transpose (= minor-orientation ::s/up)
            M (generate major-size major-size
                        #(core-get-in s (if need-transpose [%2 %1] [%1 %2])))]
        (k M #(->structure % major-orientation minor-orientation need-transpose)))
      (u/illegal "structure is not square"))))

(defn square-structure-operation
  "Applies matrix operation f to square structure s, returning a structure of the same
  type as that given."
  [s f]
  (square-structure-> s (fn [m ->s] (->s (f m)))))

(defn ^:private M*u
  "Multiply a matrix by an up structure on the right. The return value is up."
  [^Matrix m u]
  (when (not= (.-c m) (count u))
    (u/illegal "matrix and tuple incompatible for multiplication"))
  (apply s/up
         (map (fn [i]
                (reduce g/+ (for [k (range (.-c m))]
                              (g/* (core-get-in (.-v m) [i k])
                                   (get u k)))))
              (range (.-r m)))))

(defn ^:private d*M
  "Multiply a matrix by a down tuple on the left. The return value is down."
  [d ^Matrix m]
  (when (not= (.-r m) (count d))
    (u/illegal "matrix and tuple incompatible for multiplication"))
  (apply s/down
         (map (fn [i]
                (reduce g/+ (for [k (range (.-r m))]
                              (g/* (get d k)
                                   (core-get-in (.-v m) [i k])
                                   ))))
              (range (.-c m)))))

(defn ^:private kronecker
  [i j]
  (if (= i j) 1 0))

(def ^:dynamic *careful-conversion* true)

(defn s->m
  "Convert the structure ms, which would be a scalar if the (compatible) multiplication
  (* ls ms rs) were performed, to a matrix."
  [ls ms rs]
  (when *careful-conversion*
    (assert (v/numerical? (g/* ls (g/* ms rs)))))
  (let [ndowns (s/dimension ls)
        nups (s/dimension rs)]
    (generate ndowns nups
              #(g/* (s/unflatten (map (partial kronecker %1) (range)) ls)
                    (g/* ms
                         (s/unflatten (map (partial kronecker %2) (range)) rs))))))

;; (I wonder if tuple multiplication is associative...)

(defn nth-row [^Matrix m i]
  (apply s/up ((.-v m) i)))

(defn nth-col [^Matrix m j]
  (apply s/up (map #(% j) (.-v m))))

(defn diagonal [m]
  {:pre [(square? m)]}
  (let [rows  (.-r ^Matrix m)
        elems (.-v ^Matrix m)]
    (apply s/up (map #(core-get-in elems [% %])
                     (range 0 rows)))))

(defn m->s
  "Convert the matrix m into a structure S, guided by the requirement that (* ls S rs)
  should be a scalar"
  [ls ^Matrix m rs]
  (let [ncols (.-c m)
        col-shape (s/compatible-shape ls)
        ms (s/unflatten (for [j (range ncols)]
                          (s/unflatten (nth-col m j) col-shape))
                        (s/compatible-shape rs))]
    (when *careful-conversion*
      (assert (v/numerical? (g/* ls (g/* ms rs)))))
    ms))

(defn s:transpose
  [ls ms rs]
  (m->s rs (transpose (s->m ls ms rs)) ls))

(defn ^:private vector-disj
  "The vector formed by deleting the i'th element of the given vector."
  [v i]
  (vec (concat (take i v) (drop (inc i) v))))

(defn without
  "The matrix formed by deleting the i'th row and j'th column of the given matrix."
  [^Matrix m i j]
  (->Matrix (dec (.-r m)) (dec (.-c m))
            (mapv #(vector-disj % j)
                  (vector-disj (.-v m) i))) )

(defn ^:private checkerboard-negate
  [s i j]
  (if (even? (+ i j)) s (g/negate s)))

(defn dimension [m]
  {:pre [(square? m)]}
  (.-r ^Matrix m))

(defn trace
  "Computes the trace of m, which must be square. Generic operations are
  used, so this works on symbolic square matrix."
  [m]
  {:pre [(square? m)]}
  (let [rows  (.-r ^Matrix m)
        elems (.-v ^Matrix m)]
    (transduce (map #(core-get-in elems [% %]))
               g/+
               (range 0 rows))))

(defn determinant
  "Computes the determinant of m, which must be square. Generic
  operations are used, so this works on symbolic square matrix."
  [^Matrix m]
  {:pre [(square? m)]}
  (let [v (.-v m)]
    (condp = (.-r m)
      0 m
      1 ((v 0) 0)
      2 (let [[[a b] [c d]] v]
          (g/- (g/* a d) (g/* b c)))
      (reduce g/+ (map g/*
                       (cycle [1 -1])
                       (v 0)
                       (for [i (range (.-r m))]
                         (determinant (without m 0 i))))))))

(defn cofactors
  "Computes the matrix of cofactors of the given structure with the
  same shape, if s is square."
  [^Matrix m]
  {:pre [(square? m)]}
  (let [r (.-r m)
        v (.-v m)]
    (cond (< r 2) m
          (= r 2) (let [[[a b] [c d]] v]
                    (->Matrix 2 2 [[d (g/negate c)]
                                   [(g/negate b) a]]))
          :else (generate r r
                          #(-> m (without %1 %2) determinant (checkerboard-negate %1 %2))))))

(defn invert
  "Computes the inverse of a square matrix."
  [^Matrix m]
  {:pre [(square? m)]}
  (let [r (.-r m)
        v (.-v m)]
    (condp = r
      0 m
      1 (->Matrix 1 1 [[(g/invert ((v 0) 0))]])
      (let [^Matrix C (cofactors m)
            Δ (reduce g/+ (map g/* (v 0) (-> C .-v first)))]
        (fmap #(g/divide % Δ) (transpose C))))))

(defn s:inverse
  [ls ms rs]
  (m->s (s/compatible-shape rs)
        (invert (s->m ls ms rs))
        (s/compatible-shape ls)))

(defn I
  "Return the identity matrix of order n."
  [n]
  (generate n n #(kronecker %1 %2)))

(defn characteristic-polynomial
  "Compute the characteristic polynomial of the square matrix m, evaluated
  at x. Typically x will be a dummy variable, but if you wanted to get the
  value of the characteristic polynomial at some particular point, you could
  supply a different expression."
  [^Matrix m x]
  (let [r (.-r m)
        c (.-c m)]
    (when-not (= r c) (u/illegal "not square"))
    (determinant (g/- (g/* x (I r)) m))))

(defmethod g/negate [::matrix] [a] (fmap g/negate a))
(defmethod g/sub [::matrix ::matrix] [a b] (elementwise g/- a b))
(defmethod g/add [::matrix ::matrix] [a b] (elementwise g/+ a b))
(defmethod g/mul [::matrix ::matrix] [a b] (mul a b))
(defmethod g/mul [::v/scalar ::matrix] [n a] (fmap #(g/* n %) a))
(defmethod g/mul [::matrix ::v/scalar] [a n] (fmap #(g/* % n) a))
(defmethod g/mul [::matrix ::s/up] [m u] (M*u m u))
(defmethod g/mul [::s/down ::matrix] [d m] (d*M d m))
(defmethod g/div [::s/up ::matrix] [u M] (M*u (invert M) u))
(defmethod g/exp [::square-matrix] [m] (series/exp-series m))
(defmethod g/cos [::square-matrix] [m] (series/cos-series m))
(defmethod g/sin [::square-matrix] [m] (series/sin-series m))
(defmethod g/tan [::square-matrix] [m] (series/tan-series m))
(defmethod g/sec [::square-matrix] [m] (series/sec-series m))
(defmethod g/acos [::square-matrix] [m] (series/acos-series m))
(defmethod g/asin [::square-matrix] [m] (series/asin-series m))
(defmethod g/atan [::square-matrix] [m] (series/atan-series m))
(defmethod g/cosh [::square-matrix] [m] (series/cosh-series m))
(defmethod g/sinh [::square-matrix] [m] (series/sinh-series m))
(defmethod g/tanh [::square-matrix] [m] (series/tanh-series m))
(defmethod g/asinh [::square-matrix] [m] (series/asinh-series m))
(defmethod g/atanh [::square-matrix] [m] (series/atanh-series m))
(defmethod g/simplify [::matrix] [m] (->> m (fmap g/simplify) v/freeze))

(defmethod g/invert [::matrix] [m] (invert m))

(defmethod g/transpose [::matrix] [m] (transpose m))
(defmethod g/trace [::square-matrix] [m] (trace m))
(defmethod g/determinant [::square-matrix] [m] (determinant m))
(defmethod g/dimension [::square-matrix] [m] (dimension m))
(defmethod g/dimension [::column-matrix] [^Matrix m] (.-c m))
(defmethod g/dimension [::row-matrix] [^Matrix m] (.-r m))

(defmethod g/determinant [::s/structure] [s]
  (square-structure-> s (fn [m _] (determinant m))))

(defmethod g/trace [::s/structure] [s]
  (square-structure-> s (fn [m _] (trace m))))

(defmethod g/invert [::s/structure] [a]
  (let [a' (square-structure-operation a invert)]
    (if (= (s/orientation a') (s/orientation (first a')))
      (s/opposite a' (map #(s/opposite a' %) a'))
      a')))
