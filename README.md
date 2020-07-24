# Cards generator to teach your baby math

**TL;DR:** [Download](demo/cards.pdf) 'ready to print' document.

Inspired by "How to teach your baby math" (Glenn Doman & Janet Doman).

## Prerequisites

### Install packages

The following packages must be installed prior to using the tool.

* Git & Git LFS
* Imagemagick
* Inkscape
* Java JDK
* Kotlin compiler
* PDFTK

Here is the commands for Ubuntu 20.04 LTS.

```shell
# sudo apt install git git-lfs imagemagick inkscape pdftk
# sudo snap install --classic kotlin
```

### Enable PDF encoding

Imagemagick policy is not allowed to create PDF by default. To enable it,
start editing `/etc/ImageMagick-6/policy.xml` with the following command.

```shell
# sudo gedit /etc/ImageMagick-6/policy.xml
```

Find 'pattern="PDF"' and make sure the corresponding line looks similar.

```xml
  <policy domain="coder" rights="read | write" pattern="PDF" />
```

In case you are not able to find the pattern above, just add the policy line above
into the file (before `</policymap>`).

## Using the tool

Execute the following command and relax. It takes about one and half hour to complete the rendering.

```shell
# make
```

Find 'ready to print' output at `output/cards.pdf`. Make sure that your setup meets the following
requirements prior to print the file.

* Your printer supports large sizes of paper (A3) and color mode.
* The color mode is on.
* The size of paper is selected correctly.
* '2-Sided printing' is on.
* Page must be centered.

And remember: if you are not sure, print first two pages first.

Every execution makes unique (random) set of cards.

![Front](demo/front.png)
![Back](demo/back.png)

Enjoy!

