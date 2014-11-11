;; Land of Lisp, ch. 7
;; Translated into Clojure from Common Lisp
;; http://landoflisp.com/wumpus.lisp

(require '[clojure.set :as set])
(def *congestion-city-nodes* nil) 
(def *congestion-city-edges* nil)
(def *visited-nodes* nil)
(def *node-num* 30)
(def *edge-num* 45)
(def *worm-num* 3)
(def *cop-odds* 15)

(defn random-node []
  (inc (rand-int *node-num*)))

(defn edge-pair [a b] 
  (if (not= a b)
    (list (list a b) (list b a))))

(defn make-edge-list []
  (concat
   (loop [n *edge-num*
          coll nil] 
     (if-not (= n 0)
       (recur (dec n) (concat (edge-pair (random-node) (random-node)) coll))
       coll))))

(defn direct-edges
  "For a given node, return a list of all connected edges as
  determined by `first` of edge."
  [node edge-list]
  (filter #(= (first %)  node) edge-list))

(defn get-connected 
  "For a given node, return a list of directly and indirectly
  connected nodes. Unconnected nodes will always return themselves."
  [node edge-list]
  (let [visited (atom #{})]
    (letfn [(traverse [node]
              (if-not (@visited node)
                (do #_(swap! visited conj node)
                    (doall
                     (map (fn [edge] 
                            (swap! visited conj node)
                            (traverse ((comp first rest) edge)))
                          (direct-edges node edge-list))))))]
      (traverse node))
    (if-not (empty? @visited) 
      @visited
      #{node})))

;; Returns all nodes, clusted into sets with which nodes they connect
;; directly and indirectly.
(defn find-islands [nodes edge-list] 
  (let [islands (atom #{})]
    (letfn [(find-island [nodes]
              (do
                (let [connected (get-connected (first nodes) edge-list)
                      unconnected (set/difference (apply hash-set nodes) connected)] 
                  (swap! islands conj connected)
                  (when (seq unconnected)
                    (find-island unconnected)))))] 
      (find-island nodes))
    @islands))

(defn connect-with-bridges [islands] 
  (when (seq (rest islands))
    (concat (edge-pair (ffirst islands) ((comp ffirst rest) islands)) 
            (connect-with-bridges (rest islands)))))

(defn connect-all-islands [nodes edge-list]
  (concat (connect-with-bridges (find-islands nodes edge-list)) edge-list))

;; Returns alist. Not good Clojure style, but doing direct translation.
(defn edges-to-alist [edge-list]
  (map (fn [node1] 
         (cons node1
               (map (fn [edge]
                      (rest edge))
                    (distinct (direct-edges node1 edge-list))))) 
       (distinct (map first edge-list))))

;; Receives alist of edges and list of edges with cops. Note to self,
;; empty sets are not falsey.
(defn add-cops [edge-alist edges-with-cops]
  (println (apply hash-set edges-with-cops))
  (map (fn [x]
         (let [node1 (first x)
               node1-edges (rest x)]
           (cons node1
                 (map (fn [edge]
                        (let [node2 (first edge)]
                          (do 
                            (println (apply hash-set (edge-pair node1 node2))) 
                            (if (seq (set/intersection (apply hash-set (edge-pair node1 node2))
                                                   (apply hash-set edges-with-cops)))
                              (list node2 'cops)
                              edge))))
                      node1-edges))))
       edge-alist))

;; Predicate to filter uses a clever trick to pass off a boolean as a function.
(defn make-city-edges []
  (let [nodes  (range 1 (inc *node-num*))
        edge-list (connect-all-islands nodes (make-edge-list)) 
        cops (filter (fn [_] (zero? (rand-int *cop-odds*))) edge-list)]
    (add-cops (edges-to-alist edge-list) cops)))

