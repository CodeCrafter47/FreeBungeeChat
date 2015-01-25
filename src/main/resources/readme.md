Description
===========

The chat plugin I use on my server

Permissions
===========

there are no permissions

Commands
========

`/tell`, `/message`, `/w`, `/whisper`, `/msg` - send a private message
`/r` - reply to a previous message
`/ignore` - don't receive messages from a specific player

Text Format
===========

You can create links and access other formatting options with a syntax similar to bbcode. Vanilla color codes still work.

For example [b]this is bold[/b], [i]this is italic[/i], [u]this is underlined[/u] and [s]this is crossed out[/s].
The difference between the above and making something &lbold&r the vanilla way is, that the above makes all the enclosed
text bold, while &b makes bold everything until reaching the next color code.
Same for [color=...]

How links will work is easy to guess, e.g. it's just [url]spigotmc.org[/url] or [url=click here]spigotmc.org[/url].
Executing commands works similar [command=/tp CodeCrafter47]click here[/command].

Suggesting commands works with [suggest=/tp ]...[/suggest]
To create tooltips d [hover=Text magically appears when moving the mouse over]this[/hover].

It is possible to use [nocolor][/nocolor] to prevent the use of legacy color codes in a block;
[nobbcode][/nobbcode] will prevent the use of bbcode in a block;