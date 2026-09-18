# AIDE Next Changelog

{{#releases}}
## [{{name}}](https://github.com/hishamalmushrea-cloud/IDE/releases/tag/{{name}}) ({{date}})

{{#sections}}
### {{name}}

{{#commits}}
* [{{#short5}}{{SHA}}{{/short5}}](https://github.com/hishamalmushrea-cloud/IDE/commit/{{SHA}}) {{message.shortMessage}} ({{authorAction.identity.name}})

{{/commits}}
{{^commits}}
No changes.
{{/commits}}
{{/sections}}
{{^sections}}
No changes.
{{/sections}}
{{/releases}}
{{^releases}}
No releases.
{{/releases}}
