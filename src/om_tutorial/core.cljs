(ns om-tutorial.core 
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(println "Hello World!")

(def app-state (atom {:count 0
                      :app/title "Animals"
                      :animals/list
                      [[1 "Ant"] [2 "Antelope"] [3 "Bird"] [4 "Cat"] [5 "Dog"]
                       [6 "Lion"] [7 "Mouse"] [8 "Monkey"] [9 "Snake"] [10 "Zebra"]]}))

(defui HelloWorld
  Object
  (render [this]
    (dom/div nil (get (om/props this) :title))))

(def hello (om/factory HelloWorld))

(js/ReactDOM.render
  (apply dom/div nil
         (map #(hello {:react-key %
                       :title (str "Hello" %)})
              (range 3)))
  (gdom/getElement "app"))

;; Use reconciler properly
(defmulti read (fn [env key params] key))

(defmethod read :default
  [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [[_ value] (find st key)]
      {:value value}
      {:value :not-found})))

(defmethod read :animals/list
  [{:keys [state] :as env} key {:keys [start end]}]
  {:value (subvec (:animals/list @state) start end)})

(defn mutate [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:count]}
     :action #(swap! state update-in [:count] inc)}
    {:value :not-found}))

(defui Counter
  static om/IQuery
  (query [this]
    [:count])
  Object
  (render [this]
    (let [{:keys [count]} (om/props this)]
      (dom/div nil
         (dom/span nil (str "Count: " count))
         (dom/button
           #js {:onClick
                (fn [e] (om/transact! this '[(increment)]))}
           "Click me!")))))

(defui AnimalsList
  static om/IQueryParams
  (params [this]
    {:start 0 :end 10})
  static om/IQuery
  (query [this]
    '[:app/title (:animals/list {:start ?start :end ?end})])
  Object
  (render [this]
    (let [{:keys [app/title animals/list]} (om/props this)]
      (dom/div nil
         (dom/h2 nil title)
         (apply dom/ul nil
            (map
              (fn [[i name]]
                (dom/li nil (str i ". " name)))
              list))))))

(def reconciler
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read :mutate mutate})}))

(def reconciler1
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read})}))

(om/add-root! reconciler
   Counter (gdom/getElement "app1"))

(om/add-root! reconciler1
   AnimalsList (gdom/getElement "app2"))


;; Studying Identity & Normalization
(def init-data
  {:list/one [{:name "John" :points 0}
              {:name "Mary" :points 0}
              {:name "Bob"  :points 0}]
   :list/two [{:name "Mary" :points 0 :age 27}
              {:name "Gwen" :points 0}
              {:name "Jeff" :points 0}]})

(defmulti read1 om/dispatch)

(defn get-people [state key]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st key))))

(defmethod read1 :list/one
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmethod read1 :list/two
  [{:keys [state] :as env} key params]
  {:value (get-people state key)})

(defmulti mutate1 om/dispatch)

(defmethod mutate1 'points/increment
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
            [:person/by-name name :points]
            inc))})

(defmethod mutate1 'points/decrement
  [{:keys [state]} _ {:keys [name]}]
  {:action
   (fn []
     (swap! state update-in
            [:person/by-name name :points]
            #(let [n (dec %)] (if (neg? n) 0 n))))})

(defui Person
  static om/Ident
  (ident [this {:keys [name]}]
    [:person/by-name name])
  static om/IQuery
  (query [this]
    '[:name :points :age])
  Object
  (render [this]
    (println "Render Person" (-> this om/props :name))
    (let [{:keys [points name] :as props} (om/props this)]
      (dom/li nil
              (dom/label nil (str name ", points: " points))
              (dom/button
                #js {:onClick
                     (fn [e]
                       (om/transact! this
                                     `[(points/increment ~props)]))}
                "+")
              (dom/button
                #js {:onClick
                     (fn [e]
                       (om/transact! this
                                     `[(points/decrement ~props)]))}
                "-")))))

(def person (om/factory Person {:keyfn :name}))

(defui ListView
  Object
  (render [this]
    (println "Render ListView" (-> this om/path first))
    (let [list (om/props this)]
      (apply dom/ul nil
             (map person list)))))

(def list-view (om/factory ListView))

(defui RootView
  static om/IQuery
  (query [this]
    (let [subquery (om/get-query Person)]
      `[{:list/one ~subquery} {:list/two ~subquery}]))
  Object
  (render [this]
    (println "Render RootView")
    (let [{:keys [list/one list/two]} (om/props this)]
      (apply dom/div nil
             [(dom/h2 nil "List A")
              (list-view one)
              (dom/h2 nil "List B")
              (list-view two)]))))

(def reconciler2
  (om/reconciler
    {:state  init-data
     :parser (om/parser {:read read1 :mutate mutate1})}))

(om/add-root! reconciler2
              RootView (gdom/getElement "app3"))
