# rpg-action

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

Copy profiles.clj.sample to profiles.clj

Replace the boilerplate slack signing secret with the one from your slack application

## Running

To start a web server for the application, run:

    lein ring server

## MVP

A user will be able to save a Savage World dice action.

```
/rpgaction save <action name> <dice rules> <modifiers>
```

A user will be able to use one of their saved actions

```
/rpgaction <action name> <modifiers>
```

Draw initiative cards in a Savage World setting. Deck will shuffle when a Joker is drawn.

```
/rpgaction register <player1> <number of cards> <player2> <number of cards> ...
/rpgaction draw
```

Examples:

```
/rpgaction save attack 2d8! 2
```

```
/rpgaction attack -1

Total: 14 + 1 = 15
[4]
[8 2]
```

```
/rpgaction register John 1 Jane 3
/rpgaction draw

John: 5 of Hearts
Jane 10 of Clubs, 2 of Diamonds, Black Joker
```

## Future Ideas

1) Dice actions from other RPG rule sets (like dice pooling)
