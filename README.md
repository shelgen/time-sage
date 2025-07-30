# Time Sage

Time Sage is a Discord bot for planning your weekly TTRPG sessions.

## Background

Time Sage was written to solve a problem within our TTRPG group, where we had multiple campaigns and wanted to run one
to two sessions a week based on availability of players and GM's within those campaigns.

Time Sage was created to solve several parts of this problem that were tedious to do manually every week:

- Collecting updated information about who was available when
- Figuring out if a certain campaign could be played on a certain day based on availabilities of participants
- Figuring out the ideal mix of sessions in a week based on certain parameters (less ideal time slots, two days in a
  row, etc.)

## How it works

Every week, Time Sage will post a message in a configured channel that participants can interact with.
Participants will be able to set the days they are available or unavailable, as well as if they're available but
it's not ideal ("if need be"). Participants can also say they're not available at all that week, or only available for
one session.

Every day for the remainder of the week, Time Sage will remind any participants who have not responded at all to do so.

Once enough replies have been collected, a command can be used to list all possible plans for the week, sorted by
how close to ideal they are (their "Score"). Navigating through the alternatives, each can be publicly suggested
and subsequently concluded with to set the plan for the coming week. The resulting message will show exactly who will
participate in what at what time.

## Features

- Configuration of several activities with different participants
- Automatic weekly planning prompts in a specified Discord channel
- Time Zone sensitive display of start time, so everyone can easily see their local time
- Finding and concluding with the best plan for next week

### Known limitations

- Monday is the start of the week
- All sessions are planned at 18:30 UTC
- Simultaneous interactions (two people pressing buttons at the same time) are not handled particularly well

## Installing and running Time Sage

Time Sage is created to be very easy and flexible to run. It runs as a Java JAR file and saves all its configuration and
data in local JSON files. This means it can basically be run on any machine, including cloud VM's and the like.

#### Prerequisites

You need a Java JDK/JRE of at least version 21 in order to build/run the bot.

You need to set up a Discord Application with a [Bot User](https://discord.com/developers/docs/topics/oauth2#bot-users).
You must invite this bot user to your server and channel. It needs the permission `Send Messages`.

Unfortunately, I think it's beyond the scope of this documentation to explain how to accomplish this, but there should
be plenty of tutorials and the like online for it.

#### Building and installing

Clone this repo and run the following command or its equivalent on your system:

```bash
./mvnw verify
```

You should end up with a file in the `target` folder called `time-sage-(version)-jar-with-dependencies.jar`. Copy this
file to wherever you want the bot to run.
Create a file in the same directory as the .jar file called `time-sage-bot.token`. In this, paste the token for your
Discord Bot user (see Prerequisites). Make sure to limit access to this file and token.

#### Running

Simply run the .jar file using

```bash
java -jar time-sage-(version)-jar-with-dependencies.jar
```

If you're running it on a Linux VM instance through SSH, you want to add `nohup` and `&` to ensure that it runs in the
background and keeps running after you disconnect the SSH session:

```bash
nohup java -jar time-sage-(version)-jar-with-dependencies.jar &
```

While being used, the bot will create a directory named `time-sage`. In it, it will store configuration and availability
responses in 

The bot will log everything to the console, including every interaction by every user. If you ran it with `nohup`, the
log will instead be in the file `nohup.out`.

## Using Time Sage

### Permissions

Time Sage is operated using slash commands. By default, all server admins can do these. if you're not a server admin,
someone who is needs to give you specific permission to use Time Sage's slash commands.

### Configuration

In a channel where the bot is present, use the command `/tsconfigure` to receive a configuration message from the bot.
Use the controls in here to set up your activities and their participants, among other things. Finally, enable
Time Sage to make it start its planning cycle.

### Planning

When you're happy with the responses for next week, use `/tsschedule` to have Time Sage give you a list of alternatives
for the plan for next week. Through this interface, you can suggest one or more of them publicly, which will also give
buttons to conclude with them.

## Developer notes
Time Sage is written in Kotlin using the [JDA Library](https://github.com/discord-jda/JDA).

To simplify interactivity,
a sort of framework has been written thinking of Time Sage's messages as UI, where each particular view is called a
`Screen`. Each screen in turn has particular components with behavior. The behavior of a button depends on context,
so both the screen and components have contextual data that are serialized into their ID's in Discord. This allows
the state of each component to live in Discord's messages and custom ID's instead of having to be stored in a database
or the like.

This has proved useful to make somewhat flexible and reliable UI's, and may be split into a separate library at some
point if I'm going to make more Discord bots with interactable messages.

## Possible future functionality

- Planning of other stuff than TTRPG's
- Allowing to plan per month instead of per week
- One-time planning feature (like you'd do with Doodle, Strawpoll, etc.)
