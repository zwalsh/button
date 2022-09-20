# Notes

## Send Notifications

Send texts to people when the button is being pressed.

### Requirements

- I want to know when people are pressing the button, so that I can join in and press too.
- I want to get a text message if the button is being pressed for the first time in the last hour.
- I need to limit how many messages I'm sending, so I don't go broke.
- I need to have an admin page that can control some things:
  - Remove phone numbers
  - Pause messages
  - ...

### To Do

- [x] Continuous Delivery
- [x] Healthchecks.io
- [x] Set up persistence
- [x] Set up an admin page
  - [ ] Show some fun stuff
  - [ ] List phone numbers
  - [ ] Pause texting
  - [ ] Update limits
- [x] Count presses
- [x] Set up a registration page
- [ ] Set up an opt-out page
  - [ ] Delete numbers on request 
- [x] Integrate with Twilio
- [x] Validate phone numbers
- [x] Send messages, configurably
- [ ] Set up Twilio webhooks
- [ ] Set up sentry monitoring
- [ ] On register, check if number exists
