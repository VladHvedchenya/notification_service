# Post-created notifications

Redis channel `post.created` carries JSON with `postId`, `authorId`, and optional
`projectId`. The first two fields are required. Both services use the same event
contract without sharing JPA entities or Java serialization.

The listener validates the event, loads the author through user_service, builds
the message, and selects a NotificationService matching the user's preference.
Email is currently the only implemented delivery channel. Missing or unsupported
preferences fail explicitly rather than silently choosing another channel.

Messages are built from UTF-8 resource bundles. Russian is currently selected
explicitly by the listener; user-specific locale is not implemented yet.

Set MAIL_USERNAME and MAIL_PASSWORD in the local run configuration. For Gmail,
MAIL_PASSWORD is an app password, not the account password or a two-factor code.
Never commit credentials or share a run configuration containing them.

The recipient comes from user_service, not from the Redis payload. Before a
manual test, set the selected development user's email to your own address and
their preference to EMAIL. Run only one consumer instance unless duplicate
notifications from Pub/Sub fan-out are intentional.

Redis Pub/Sub has no message persistence, acknowledgements, or retry mechanism
here. A successful PUBLISH does not prove that SMTP delivery succeeded. If the
consumer is offline, its messages are lost. Delivery failures currently appear
in the listener container logs; no exactly-once guarantee is claimed.

Unit tests mock Feign and JavaMailSender, validate the event and sender selection,
and check real Russian and English resource bundles. They never send real email.

This repository's feature/notification-handler-event branch contains notification
handling only. Post creation and transaction-aware publication belong in the
separate post_service repository and its own feature branches.
