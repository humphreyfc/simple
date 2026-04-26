# Contribution guide

We're thrilled you're interested in contributing to SimpleEmail! There are lots of ways to get involved - see below for details.

## Documentation

Contributions to this document and the frequently asked questions
are preferred in the form of [pull requests][pull-requests].

## Translations

* SimpleEmail can be translated via [Weblate](https://hosted.weblate.org/projects/simple-email/). You can log in there to help or open a [pull requests][pull-requests].
* If your language is not listed, please open a [issue][] or send a message through [email](distopico@riseup.net)

## Source code

Source code contributions are welcome, there's always work to be done on the SimpleEmail codebase, building from source code is
straightforward with `./gradlew assembleDebug` Here are the general rules:

* Write good commit messages, we follow the [conventional commits][commits]
* Make sure the linter (`./gradlew lint`) passes (in progress)
* Stick to [F-Droid contribution guidelines](https://f-droid.org/wiki/page/Inclusion_Policy)
* Make changes on a separate branch, not on the master branch. This is commonly known as *feature branch workflow* and `develop` as the base
* Send patches via [Pull Requests][pull-requests], you branch should follow this convention: `[type]-[description]` example `feat-improve-performance`
* When submitting changes, you confirm that your code is licensed under the terms of the [GNU General Public License v3](https://www.gnu.org/licenses/gpl-3.0.html).
* Please test (compile and run) your code before you submit changes

 [issue]: https://framagit.org/dystopia-project/simple-email/issues
 [commits]: https://www.conventionalcommits.org/en/v1.0.0-beta.2/
 [pull-requests]: https://framagit.org/dystopia-project/simple-email/merge_requests
