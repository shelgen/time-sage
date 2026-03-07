The goal of Time Sage is to help users conclude with a **plan** for a specific **target period**, given possible
**activities**, and **time slots** in that **target period**, and collecting the **availabilities** of each
**activity**'s **members**.

When all **availabilities** have been collected, Time Sage will present the users with all logically possible **plans**
maximizing **attendance** for each **member** across all **activities**. Time Sage sorts these options for the users by
**scoring** them based on how good they are, the definition of which is in its hardcoded logic.

Time Sage collects **availabilities** through interactive **availability messages**, which can be either be a
**single message** or a whole **thread**, the latter being necessary only because Discord messages can only be so long.
If there a few enough **time slots** in the **target period**, a **thread** is not necessary.

Time Sage operates independently for each **tenant**, which is bound by a specific **text channel** in a
specific **Discord server**. Nothing that happens for one **tenant** can affect another **tenant**.

- A **target period** is the date range being planned for with an arbitrary start and end date.
- A **time slot** is a **starting date and time** within a **target period**.
- An **activity** is a a named event that can be planned for at a specific time.
- An **activity** has one or more **members**, which is a Discord user who may participate that activity. **Members**
  can be optional or required.
- A **planned session** is a **session** with a set of participants.
- A **session** is an activity at a specific **time slot**.
- A **plan** consists of zero of more **planned sessions**.
