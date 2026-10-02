(ns flashcards
  "A simple command-line flashcard app.

  Run with:  clojure -M flashcards.clj [clustering.edn]
        or:  bb flashcards.clj [clustering.edn]"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Deck loading

(def default-deck
  "Used when no deck file is found."
  [{:front "What does (first [1 2 3]) return?"      :back "1"}])

(defn load-deck
  "Reads a vector of {:front ... :back ...} maps from an EDN file,
  falling back to the default deck if the file doesn't exist."
  [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (do (println (str "No deck found at " path ", using the built-in sample deck."))
        default-deck)))

;; ---------------------------------------------------------------------------
;; Input helpers

(defn prompt
  "Prints msg, waits for a line of input and returns it trimmed.
  Returns nil at end of input."
  [msg]
  (print msg)
  (flush)
  (some-> (read-line) str/trim))

;; ---------------------------------------------------------------------------
;; Studying

(defn ask-card
  "Shows one card and returns :right, :wrong or :quit."
  [{:keys [front back]}]
  (println)
  (println "Q:" front)
  (prompt "   (press Enter to reveal) ")
  (println "A:" back)
  (loop []
    (case (str/lower-case (or (prompt "   Did you get it? [y/n/q] ") "q"))
      "y" :right
      "n" :wrong
      "q" :quit
      (recur))))

(defn study-round
  "Quizzes every card once in random order.
  Returns {:missed [cards...] :quit? boolean}."
  [cards]
  (loop [[card & more :as remaining] (shuffle cards)
         missed []]
    (if-not card
      {:missed missed :quit? false}
      (case (ask-card card)
        :right (recur more missed)
        :wrong (recur more (conj missed card))
        :quit  {:missed (into missed remaining) :quit? true}))))

(defn study
  "Runs rounds until every card has been answered correctly (or you quit)."
  [deck]
  (loop [cards deck
         round 1]
    (println (str "\n=== Round " round ": " (count cards) " card(s) ==="))
    (let [{:keys [missed quit?]} (study-round cards)]
      (cond
        quit?
        (println "\nStopped early. See you next time!")

        (empty? missed)
        (println (str "\nAll " (count deck) " cards learned. Nice work!"))

        :else
        (do (println (str "\nGot " (- (count cards) (count missed))
                          "/" (count cards)
                          ". Going over the " (count missed) " you missed..."))
            (recur missed (inc round)))))))

;; ---------------------------------------------------------------------------
;; Entry point

(defn -main [& args]
  (let [deck (load-deck (or (first args) "clustering.edn"))]
    (if (empty? deck)
      (println "The deck is empty. Add some cards and try again.")
      (study deck))))

(apply -main *command-line-args*)