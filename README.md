# rpg-action

## Prerequisites

You will need [Leiningen][] 2.0.0 or above installed.

[leiningen]: https://github.com/technomancy/leiningen

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

## Future Ideas

1) Savage World DM actions like drawing initiative cards
2) Dice actions from other RPG rule sets (like dice pooling)
