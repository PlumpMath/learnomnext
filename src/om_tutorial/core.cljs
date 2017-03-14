(ns om-tutorial.core 
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(println "Hello World!")

(def app-state (atom {:count 0}))

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

;; A Naive Design
(defui Counter
  Object
  (render [this]
    (let [{:keys [count]} (om/props this)]
      (dom/div nil
         (dom/span nil (str "Count: " count))
         (dom/button
           #js {:onClick
                (fn [e]
                  (swap! app-state update-in [:count] inc))}
           "Click me!")))))

(def reconciler
  (om/reconciler
    {:state app-state}))

(om/add-root! reconciler
   Counter (gdom/getElement "app1"))



