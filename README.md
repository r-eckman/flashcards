# Clustering Algorithms Flashcards

This is a simple clojure flashcard application I am
using to study for a class. If I have any mistakes
in the flashcards, feel free to open a PR. 

Code/flashcards were generated with the assistance
of claude Opus 5.5, inside of a project where it was
instructed to use the textbook and slides as sources
of truth.

To run, use the command `clojure -M flashcards.clj`. 

It shuffles the cards and asks if you got the
question right. It will continue to go through the
deck until all of the questions have been answered
correctly.

## Commands

```
clojure -M flashcards.clj            # study every card (default)
clojure -M flashcards.clj topics     # list topics with how many cards cover each
clojure -M flashcards.clj chapters   # list chapters with how many cards cover each
```

## Filtering

Study a subset of the deck with `--topic` and
`--chapter`:

```
clojure -M flashcards.clj --chapter 2
clojure -M flashcards.clj --topic k-means --topic fuzzy-clustering
clojure -M flashcards.clj --topic proximity --chapter 2
```

Repeating an option matches cards with any of the
given values. Combining `--topic` and `--chapter`
only includes cards that match both.

A different deck file can be given anywhere in the
command, e.g. `clojure -M flashcards.clj topics other.edn`.

## Card format

Each card in `clustering.edn` is a map:

```clojure
{:front    "Question"
 :back     "Answer"
 :topics   ["topic-a" "topic-b"]
 :chapters [1 2]}
```