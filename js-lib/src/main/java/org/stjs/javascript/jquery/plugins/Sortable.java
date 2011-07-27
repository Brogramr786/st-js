/**
 *  Copyright 2011 Alexandru Craciun, Eyal Kaspi
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.stjs.javascript.jquery.plugins;

import org.stjs.javascript.jquery.JQuery;

public interface Sortable<FullJQuery extends JQuery<?>> {
	public FullJQuery sortable();

	public FullJQuery sortable(SortableOptions<FullJQuery> options);

	public FullJQuery sortable(String methodName);

	public Object sortable(String option, String optionName);

	public FullJQuery sortable(String option, String optionName, Object value);

	public FullJQuery sortable(String option, DraggableOptions<FullJQuery> options);
}