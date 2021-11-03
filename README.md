# Live Coding Session from Liferay `/dev/24` (2021)

During a live coding session for the `/dev/24`, I decided to use the almost 2h I had to create a WeTransfer-like application. The goal is to generate discussions around anything that can happen during this process, but also see how much we can rely on Liferay out-of-the-box features to create such an application.

## Our Files

`Our Files` is the name of our WeTransfer-like application. What we want to achieve:

- Upload multiple files as guest
- Share the files to multiple recipients
- Verify the sender before sending an email notification
- Let links expire after some time

## Liferay Features

With that limited time, the less we code the better. So we're going to rely on some Liferay features to avoid reinventing the wheel and add some code to glue all of that together.

- __Liferay Forms__ for uploading files and send links
- __Workflows__ to validate emails and publish links
- __Display Page__ for downloading files