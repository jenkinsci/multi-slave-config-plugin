lastChangedSettings.get('removeProperties').each() { d ->
    span(it.mapClassToDescriptorName(d))
    br()
}
