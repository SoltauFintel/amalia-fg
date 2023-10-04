# amalia-fg: Amalia Formular Generator

`fg` is a command line tool for generating HTML files and Java code snippets for add or edit formulars.
Just specify the command file names (without path and suffix) separated by spaces on the command line.
Command files must be placed in the *project*/formular folder and have .txt as suffix.
Output are src/main/resources/template/*filename*.html and formular/*filename*_code.java.

## command file content:

indent: *indent*

version: *(nothing)*

t: *textfield id*

c: *combobox id*

k: *checkbox id*

ta: *multi-line textarea id*

e: *empty*

ok: *action link*

cancel: *cancel link*
