/*
 * Copyright (c) 2017. Nuvolect LLC
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * Contact legal@nuvolect.com for a less restrictive commercial license if you would like to use the
 * software without the GPLv3 restrictions.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not,
 * see <http://www.gnu.org/licenses/>.
 *
 */

package com.nuvolect.securesuite.webserver;

import java.io.IOException;
import java.util.HashMap;

/**
* A cache manager for MiniTemplator objects.
* This class is used to cache MiniTemplator objects in memory, so that
* each template file is only read and parsed once.
*
* <p>
* Example of how to use the template cache:<br>
* <pre>
*  private static MiniTemplatorCache miniTemplatorCache = new MiniTemplatorCache();
*
*  public static MiniTemplator getTemplate (String templateFileName, Set<String> flags) throws Exception {
*     MiniTemplator.TemplateSpecification templateSpec = new MiniTemplator.TemplateSpecification();
*     templateSpec.templateFileName = templateFileName;
*     templateSpec.conditionFlags = flags;
*     return miniTemplatorCache.get(templateSpec); };</pre>
*
* <p>
* Home page: <a href="http://www.source-code.biz/MiniTemplator">www.source-code.biz/MiniTemplator</a><br>
* Author: Christian d'Heureuse, Inventec Informatik AG, Zurich, Switzerland<br>
* Multi-licensed: EPL / LGPL.
*/
public class MiniTemplatorCache {

private HashMap<String,MiniTemplator> cache;               // buffered MiniTemplator objects

/**
* Creates a new MiniTemplatorCache object.
*/
public MiniTemplatorCache() {
   cache = new HashMap<String,MiniTemplator>(); }

/**
* Returns a cloned MiniTemplator object from the cache.
* If there is not yet a MiniTemplator object with the specified <code>templateFileName</code>
* in the cache, a new MiniTemplator object is created and stored in the cache.
* Then the cached MiniTemplator object is cloned and the clone object is returned.
* @param  templateSpec      the template specification.
* @return                   a cloned and reset MiniTemplator object.
*/
public synchronized MiniTemplator get (MiniTemplator.TemplateSpecification templateSpec)
      throws IOException, MiniTemplator.TemplateSyntaxException {
   String key = generateCacheKey(templateSpec);
   MiniTemplator mt = cache.get(key);
   if (mt == null) {
      mt = new MiniTemplator(templateSpec);
      cache.put(key, mt); }
   return mt.cloneReset(); }

private static String generateCacheKey (MiniTemplator.TemplateSpecification templateSpec) {
   StringBuilder key = new StringBuilder(128);
   if (templateSpec.templateText != null)
      key.append(templateSpec.templateText);
    else if (templateSpec.templateFileName != null)
      key.append(templateSpec.templateFileName);
    else
      throw new IllegalArgumentException("No templateFileName or templateText specified.");
   if (templateSpec.conditionFlags != null) {
      for (String flag : templateSpec.conditionFlags) {
         key.append('|');
         key.append(flag.toUpperCase()); }}
   return key.toString(); }

/**
* Clears the cache.
*/
public synchronized void clear() {
   cache.clear(); }

} // end class MiniTemplatorCache