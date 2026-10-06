(ns flashcards
  "A simple command-line flashcard app.

  Run with:  clojure -M flashcards.clj [command] [deck.edn] [options]

  Commands:
    study      Quiz yourself (the default)
    topics     List every topic with how many cards cover it
    chapters   List every chapter with how many cards cover it

  Options (study only, may be repeated):
    --topic T     Only cards tagged with topic T
    --chapter N   Only cards relevant to chapter N

  Repeating an option matches cards with any of the given values;
  combining --topic and --chapter requires a card to match both."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Deck loading

(def default-deck
  "Used when no deck file is found."
  [{:front    "What does (first [1 2 3]) return?"
    :back     "1"
    :topics   ["sequences"]
    :chapters [1]}])

(defn load-deck
  "Reads a vector of {:front :back :topics :chapters} maps from an EDN
  file, falling back to the default deck if the file doesn't exist."
  [path]
  (if (.exists (io/file path))
    (edn/read-string (slurp path))
    (do (println (str "No deck found at " path ", using the built-in sample deck."))
        default-deck)))

;; ---------------------------------------------------------------------------
;; Filtering and listing

(defn filter-deck
  "Keeps cards that share at least one topic with `topics` (if any are
  given) and at least one chapter with `chapters` (if any are given)."
  [deck topics chapters]
  (let [matches? (fn [wanted card-vals]
                   (or (empty? wanted) (some (set wanted) card-vals)))]
    (filter #(and (matches? topics (:topics %))
                  (matches? chapters (:chapters %)))
            deck)))

(defn tally
  "Returns a map of each value under key k to the number of cards it appears on."
  [deck k]
  (frequencies (mapcat k deck)))

(defn print-tally
  "Prints a tally as aligned `name  count` lines, using sort-fn to order entries."
  [counts sort-fn]
  (if (empty? counts)
    (println "Nothing to list.")
    (let [width (apply max (map (comp count str key) counts))]
      (doseq [[v n] (sort-fn counts)]
        (println (format (str "%-" width "s  %d") v n))))))

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

(def commands #{"study" "topics" "chapters"})

(defn parse-args
  "Turns command-line args into {:command :path :topics :chapters}."
  [args]
  (loop [[arg & more] args
         opts {:command "study" :path "clustering.edn" :topics [] :chapters []}]
    (cond
      (nil? arg)
      opts

      (= arg "--topic")
      (do (when-not (first more)
            (println "--topic expects a topic name.")
            (System/exit 1))
          (recur (rest more) (update opts :topics conj (first more))))

      (= arg "--chapter")
      (let [n (some-> (first more) parse-long)]
        (when-not n
          (println (str "--chapter expects a number, got: " (first more)))
          (System/exit 1))
        (recur (rest more) (update opts :chapters conj n)))

      (commands arg)
      (recur more (assoc opts :command arg))

      :else
      (recur more (assoc opts :path arg)))))

(defn -main [& args]
  (let [{:keys [command path topics chapters]} (parse-args args)
        deck (load-deck path)]
    (case command
      "topics"   (print-tally (tally deck :topics)
                              #(sort-by (juxt (comp - val) key) %))
      "chapters" (print-tally (tally deck :chapters)
                              #(sort-by key %))
      "study"    (let [cards (vec (filter-deck deck topics chapters))]
                   (if (empty? cards)
                     (println "No cards match. Try `topics` or `chapters` to see what's available.")
                     (study cards))))))

(apply -main *command-line-args*)