/*
 * Copyright (C) 2004-2015 L2J Server
 * 
 * This file is part of L2J Server.
 * 
 * L2J Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.model.html.pagehandlers;

import org.l2junity.gameserver.model.html.IHtmlStyle;
import org.l2junity.gameserver.model.html.IPageHandler;
import org.l2junity.gameserver.model.html.styles.DefaultStyle;

/**
 * Creates pager with links << | < | > | >>
 * @author UnAfraid
 */
public class NextPrevPageHandler implements IPageHandler
{
	protected final int _currentPage;
	protected final String _bypass;
	private final IHtmlStyle _style;
	
	public NextPrevPageHandler(int currentPage, String bypass)
	{
		this(currentPage, bypass, DefaultStyle.INSTANCE);
	}

	public NextPrevPageHandler(int currentPage, String bypass, IHtmlStyle style)
	{
		_currentPage = currentPage;
		_bypass = bypass;
		_style = style;
	}
	
	@Override
	public void apply(int pages, StringBuilder sb)
	{
		// Beginning
		sb.append(_style.formatBypass(_bypass + " 0", "<<", (_currentPage - 1) > 0));
		
		// Separator
		sb.append(_style.formatSeparator());

		// Previous
		sb.append(_style.formatBypass(_bypass + " " + (_currentPage - 1), "<", _currentPage > 0));
		
		sb.append(_style.formatSeparator());
		sb.append(String.format("<td align=\"center\">Page: %d/%d</td>", _currentPage + 1, pages + 1));
		sb.append(_style.formatSeparator());
		
		// Next
		sb.append(_style.formatBypass(_bypass + " " + (_currentPage + 1), ">", _currentPage < pages));

		// Separator
		sb.append(_style.formatSeparator());
		
		// End
		sb.append(_style.formatBypass(_bypass + " " + pages, ">>", (_currentPage + 1) < pages));
	}
}