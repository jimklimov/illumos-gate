/*
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the
 * Common Development and Distribution License, Version 1.0 only
 * (the "License").  You may not use this file except in compliance
 * with the License.
 *
 * You can obtain a copy of the license at usr/src/OPENSOLARIS.LICENSE
 * or http://www.opensolaris.org/os/licensing.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each
 * file and include the License file at usr/src/OPENSOLARIS.LICENSE.
 * If applicable, add the following below this CDDL HEADER, with the
 * fields enclosed by brackets "[]" replaced with your own identifying
 * information: Portions Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * ident	"%Z%%M%	%I%	%E% SMI"
 *
 * Copyright 2002 Sun Microsystems, Inc.  All rights reserved.
 * Use is subject to license terms.
 */

package com.sun.dhcpmgr.data.qualifier;

/**
 * An implementation of the qualifier type that provides an integer type.
 */
public class QualifierInteger extends QualifierTypeImpl {

    public Object parseValue(String value) {
	if (value != null) {
	    try {
		int intValue = Integer.parseInt(value.trim());
		return new Integer(intValue);
	    } catch (NumberFormatException nfe) {}
	}

	return null;
    }

    public String formatValue(String value) {
	Object object = parseValue(value);

	return (object == null) ? null : object.toString();
    }

    public Class getJavaType() {
	return Integer.class;
    }

}
