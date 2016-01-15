# JavaDoc Source Embed

JavaDoc is a wonderful tool with some annoying limitations, one of them is its inability to handle source code embeds. 
[Githubs gist](http://gist.github.com/) is an amazing tool for embeding source code but it requires JavaScript which means
it doesn't work with the popups in common IDE's and isn't as searchable. 

Gist makes source embedding trivial since you can embed a single sample in multiple methods/classes and manage the sample
itself in a centeral location. 

The JavaDoc source embed tool allows you to use gist embeds directly in your javadoc without any change! When you want to
generate local JavaDoc for use in the IDE it queries github for the gist contents and embeds it as a `<noscript>` 
alternative right into the code.

Just pass the source directory and an output directory to the jar file and then generate the javadoc based on the output
directory.
