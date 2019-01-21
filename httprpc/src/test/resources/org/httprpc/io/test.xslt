<?xml version="1.0" encoding="UTF-8"?>

<stylesheet version="1.0" xmlns="http://www.w3.org/1999/XSL/Transform">
    <output method="text" encoding="UTF-8"/>

    <template match="/root/item">
        <apply-templates>
            <value-of select="@a"/>,<value-of select="@b"/>,<value-of select="@c"/><text>&#xD;&#xA;</text>
        </apply-templates>
    </template>
</stylesheet> 
