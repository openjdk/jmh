<?xml version="1.0" encoding="utf-8"?>
<!--

    Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.

    This code is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License version 2 only, as
    published by the Free Software Foundation.  Oracle designates this
    particular file as subject to the "Classpath" exception as provided
    by Oracle in the LICENSE file that accompanied this code.

    This code is distributed in the hope that it will be useful, but WITHOUT
    ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
    FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
    version 2 for more details (a copy is included in the LICENSE file that
    accompanied this code).

    You should have received a copy of the GNU General Public License version
    2 along with this work; if not, write to the Free Software Foundation,
    Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.

    Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
    or visit www.oracle.com if you need additional information or have any
    questions.

-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:output method="text" encoding="UTF-8" standalone="yes"/>

    <xsl:template match="/">
        <!-- get CPU architecture -->
        <xsl:apply-templates select="//string">
            <xsl:with-param name="key">architecture</xsl:with-param>
        </xsl:apply-templates>
        <xsl:text>&#xa;</xsl:text>

        <!-- get fixed PMU counters mask -->
        <xsl:apply-templates select="//integer">
            <xsl:with-param name="key">fixed_counters</xsl:with-param>
        </xsl:apply-templates>
        <xsl:text>&#xa;</xsl:text>

        <!-- get configurable PMU counters mask -->
        <xsl:apply-templates select="//integer">
            <xsl:with-param name="key">config_counters</xsl:with-param>
        </xsl:apply-templates>
        <xsl:text>&#xa;</xsl:text>

        <!-- collect aliased events -->
        <xsl:apply-templates select="plist/dict" mode="aliases"/>
        <!-- collect events info -->
        <xsl:apply-templates select="plist/dict" mode="events"/>

    </xsl:template>

    <!-- print a value of <string> or <integer> nodes preceded with a specified <key> node -->
    <xsl:template match="string|integer">
        <xsl:param name="key" />
        <xsl:param name="skipKey" />
        <xsl:variable name="keyName">
            <xsl:value-of select="preceding-sibling::key[1]/text()"/>
        </xsl:variable>
        <xsl:if test="$keyName=$key">
            <xsl:if test="$skipKey!='true'">
                <xsl:value-of select="$key"/>
                <xsl:text>::</xsl:text>
            </xsl:if>
            <xsl:value-of select="text()"/>
        </xsl:if>
    </xsl:template>

    <!-- aliases extraction -->
    <xsl:template match="dict" mode="aliases">
        <xsl:variable name="name">
            <xsl:value-of select="preceding-sibling::key[1]/text()"/>
        </xsl:variable>
        <xsl:if test="$name='aliases'">
            <xsl:for-each select="string">
                <xsl:text>alias::</xsl:text>
                <!-- key contains alias -->
                <xsl:value-of select="preceding-sibling::key[1]/text()"/>
                <xsl:text>::</xsl:text>
                <!-- value of this node contains aliased event's name -->
                <xsl:value-of select="."/>
                <xsl:text>&#xa;</xsl:text>
            </xsl:for-each>
        </xsl:if>
        <!-- continue inspecting dicts recursively (if this wasn't the one we're looking for) -->
        <xsl:apply-templates select="dict" mode="aliases"/>
    </xsl:template>

    <!-- events info extraction -->
    <xsl:template match="dict" mode="events">
        <xsl:variable name="name">
            <xsl:value-of select="preceding-sibling::key[1]/text()"/>
        </xsl:variable>
        <xsl:if test="$name='events'">
            <xsl:for-each select="key">
                <xsl:text>event::</xsl:text>
                <!-- node's value contains event name -->
                <xsl:value-of select="text()"/>
                <xsl:text>::</xsl:text>
                <!-- fixed counter's mask, or empty if it's a configurable event -->
                <xsl:apply-templates select="following-sibling::dict[1]/integer">
                    <xsl:with-param name="key">fixed_counter</xsl:with-param>
                    <xsl:with-param name="skipKey">true</xsl:with-param>
                </xsl:apply-templates>
                <xsl:text>::</xsl:text>
                <!-- configurable counter's mask, if it differs from PMU-wide mask -->
                <xsl:apply-templates select="following-sibling::dict[1]/integer">
                    <xsl:with-param name="key">counters_mask</xsl:with-param>
                    <xsl:with-param name="skipKey">true</xsl:with-param>
                </xsl:apply-templates>
                <xsl:text>::</xsl:text>
                <!-- event's description -->
                <xsl:apply-templates select="following-sibling::dict[1]/string">
                    <xsl:with-param name="key">description</xsl:with-param>
                    <xsl:with-param name="skipKey">true</xsl:with-param>
                </xsl:apply-templates>
                <xsl:text>::</xsl:text>
                <!-- fallback event for a fixed counter -->
                <xsl:apply-templates select="following-sibling::dict[1]/string">
                    <xsl:with-param name="key">fallback</xsl:with-param>
                    <xsl:with-param name="skipKey">true</xsl:with-param>
                </xsl:apply-templates>
                <xsl:text>&#xa;</xsl:text>
            </xsl:for-each>
        </xsl:if>
        <!-- continue inspecting dicts recursively (if this wasn't the one we're looking for) -->
        <xsl:apply-templates select="dict" mode="events"/>
    </xsl:template>
</xsl:stylesheet>
