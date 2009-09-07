<?xml version="1.0" encoding="UTF-8"?>
<!--

    bugzillaChanges Maven Mojo. Plugin to generate changes.xml form Bugzilla info, when make release with Maven.
    Copyright (C) 2009 Autentia Real Business Solutions S.L.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

-->
<xsl:stylesheet version="2.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:fo="http://www.w3.org/1999/XSL/Format"
	xmlns:xs="http://www.w3.org/2001/XMLSchema"
	xmlns:fn="http://www.w3.org/2005/xpath-functions">
	<xsl:output method="xml" indent="yes" encoding="UTF-8" />
	<xsl:key name="version" match="version" use="version" />
	<xsl:template match="/bugzilla">
		<document>
			<properties>
				<title>
					<xsl:value-of select="title" />
				</title>
			</properties>
			<body>
				<xsl:for-each
					select="bug[not(target_milestone=preceding-sibling::bug/target_milestone)]">
					<xsl:sort select="target_milestone/@version1" data-type="number" />
					<xsl:sort select="target_milestone/@version2" data-type="number" />
					<xsl:sort select="delta_ts" data-type="text" order="descending" />
					<xsl:variable name="version">
						<xsl:value-of select="target_milestone" />
					</xsl:variable>
					<xsl:variable name="date">
						<xsl:value-of select="substring(delta_ts,1,10)" />
					</xsl:variable>
					<release>
						<xsl:attribute name="version"><xsl:value-of
								select="$version" />
						</xsl:attribute>
						<xsl:attribute name="date"><xsl:value-of
								select="$date" />
						</xsl:attribute>
						<xsl:for-each select="/bugzilla/bug[target_milestone = $version]">
							<xsl:sort select="delta_ts" data-type="text" order="descending" />
							<action>
								<xsl:attribute name="type">
									<xsl:choose>
										<xsl:when test="bug_severity = 'enhancement'">add</xsl:when>
										<xsl:otherwise>fix</xsl:otherwise>
									</xsl:choose>
								</xsl:attribute>
								<xsl:attribute name="issue">
									<xsl:value-of select="bug_id" />
								</xsl:attribute>
								<xsl:attribute name="dev">
									<xsl:value-of select="assigned_to" />
								</xsl:attribute>
								<xsl:attribute name="date">
									<xsl:value-of select="substring(delta_ts,1,10)" />
								</xsl:attribute>
								<xsl:value-of select="short_desc" />
							</action>
						</xsl:for-each>
					</release>
				</xsl:for-each>
			</body>
		</document>
	</xsl:template>
</xsl:stylesheet>
