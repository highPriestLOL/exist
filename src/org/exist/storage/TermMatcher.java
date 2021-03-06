/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2001-06 Wolfgang M. Meier
 *  wolfgang@exist-db.org
 *  http://exist.sourceforge.net
 *  
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *  
 *  $Id$
 */
package org.exist.storage;

import org.exist.dom.persistent.DocumentSet;
import org.exist.dom.persistent.NodeSet;
import org.exist.dom.QName;
import org.exist.xquery.XQueryContext;


/**
 * Implementations of this interface can be passed to method
* {@link org.exist.storage.TextSearchEngine#getNodes(XQueryContext context, DocumentSet docs, NodeSet contextSet, int axis, QName qname,
	        TermMatcher matcher, CharSequence startTerm)} 
 * to check if an index entry matches a given search term.
 * 
 * @author Wolfgang Meier (wolfgang@exist-db.org)
 */
public interface TermMatcher {
	
	public boolean matches(CharSequence term);
	
}
