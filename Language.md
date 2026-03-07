The goal of Time Sage is to help users arrive at a **plan** for a **target period**, given a set of **activities**
and the **time slots** within that period, by collecting each activity's **members**' **availabilities**.

Once the **planning process** has collected enough responses, Time Sage presents all logically possible **plans**.
Each plan consists of zero or more **sessions** and has a **score** based on certain criteria.
Time Sage sorts plans by their score so users can pick the best one.

Time Sage collects availabilities through an **availability interface** posted in the tenant's channel, which is
either an **availability message** (a single Discord message) or an **availability thread** (a thread with one
message per week in the target period). A thread is used when there are too many **time slots** to fit in a
single message.

Time Sage operates independently for each **tenant**, which is equivalent to a specific Discord text channel in
a specific Discord server. Nothing that happens for one tenant can affect another.

---

## Time and Date

- A **date** falls on exactly one **day of week**.
- A **date range** is a contiguous sequence of dates with an inclusive start and end date.
- A **time of day** is a clock time (hour and minute), independent of any specific date.
- A **time zone** determines how a point in time maps to a date and time of day.
- A **time slot** is a specific point in time, defined by a **date**, a **time of day**, and a **time zone**.

---

## Discord

- A **Discord server** contains **Discord text channels** and **Discord voice channels**.
- A **Discord text channel** and a **Discord voice channel** each exist within a **Discord server**.
- A **Discord text channel thread channel** is a thread that exists within a **Discord text channel**.
- A **Discord message** exists in a **Discord channel** (either a text channel or a thread channel).
- A **Discord user** is a member of a **Discord channel**.

---

## Tenant

A **tenant** is the unit of isolation in Time Sage. It is equivalent to exactly one **Discord text channel**.
Nothing that happens for one tenant can affect another.

---

## Configuration

A **configuration** is scoped to exactly one **tenant** and governs how Time Sage behaves for that tenant.

- **Localization** captures the tenant's **time zone** and the configured first **day of week**.
- **Periodic planning** specifies whether Time Sage automatically initiates a planning process, how many days
  before the start of the next period to do so, and the **hour of day** to trigger it. It also specifies the
  **period type**.
- A **period type** determines the shape of a target period. It is either **weekly** (a 7-day week starting on
  the configured first day of week) or **monthly** (a calendar month).
- **Reminders** specifies whether Time Sage sends reminders to members who have not yet responded, and how many
  days apart and at what hour of day those reminders are sent.
- An **activity** is a named event that can be scheduled at a time slot. A configuration may have zero or more
  activities. An activity optionally refers to a **Discord voice channel** used when creating Discord scheduled
  events for the concluded plan. An activity has a **maximum number of missing optional members**, expressing
  how many optional members may be absent while still forming a valid session.
- A **member** belongs to exactly one activity and represents a **Discord user** who may participate in that
  activity. A member is either **required** or **optional**.
- A **time slot rule** applies to a specific **day of week** and configures a **time of day**. Given a date, a
  time slot rule produces the **time slot** for that date. A configuration may have at most one time slot rule
  per day of week.

---

## Planning Process

A **planning process** exists in the scope of a single **tenant** and regards a single **date range**, referred
to as the **target period** when emphasising its role as the period being planned for. A planning process is
always in exactly one of the following states:

- **Collecting availabilities** — the availability interface has been posted and members are submitting responses.
- **Locked** — availability collection is in progress and the interface is locked against unstructured edits.
- **Concluded** — a plan has been selected and the planning process is complete.

A planning process has exactly one **availability interface**, zero or more **availability responses** (one per
member), one or more **plans** (only when Locked or Concluded), and exactly one **conclusion** (only when
Concluded).

---

## Availability Interface

An **availability interface** is posted in the tenant's text channel to collect availabilities. It is one of:

- An **availability message** — refers to a single **Discord message** containing all time slots for the target
  period. Used when the number of time slots fits in a single message.
- An **availability thread** — refers to one or more **Discord messages** inside a
  **Discord text channel thread channel**, with one message per week of the target period. Used when there are
  too many time slots for a single message.

---

## Availability Response

An **availability response** belongs to a single **member** and records that member's input for the planning
process.

- A **response for a time slot** records the member's **availability** for a specific **time slot**. Availability
  is one of:
  - **Available** — the member can attend.
  - **Available if need be** — the member can attend but prefers not to.
  - **Unavailable** — the member cannot attend.
- A **session limit** is an optional part of an availability response, capping the number of sessions the member
  wishes to attend in the target period.

---

## Plan

A **plan** is a possible scheduling outcome for a target period. A plan consists of zero or more **sessions**
and has exactly one **score**.

- A **session** regards a specific **activity** and is scheduled at a specific **time slot**. A session has one
  or more **participants**.
- A **participant** refers to a **member** who is included in a session. A participant's availability was either
  *available* or *available if need be* — the latter is captured as the participant's **if need be** flag.
- A **score** reflects the quality of a plan. Plans are ranked: fewer missing optional participants is better;
  then fewer *if need be* participants; then more sessions; then more total participant-sessions; then fewer
  pairs of sessions on consecutive days.

---

## Conclusion

A **conclusion** marks the end of a planning process. It refers to the **plan** that was selected. A conclusion
has exactly one **conclusion message** — a **Discord message** posted to the tenant's text channel that
summarises the concluded plan.

