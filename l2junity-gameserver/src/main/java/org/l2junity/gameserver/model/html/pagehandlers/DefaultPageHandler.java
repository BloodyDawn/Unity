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
 * Creates pager with links 1 2 3 | 9 10 | 998 | 999
 * @author UnAfraid
 */
public class DefaultPageHandler implements IPageHandler
{
	protected final int _currentPage;
	protected final int _pagesOffset;
	protected final String _bypass;
	private final IHtmlStyle _style;
	
	public DefaultPageHandler(int currentPage, int pagesOffset, String bypass)
	{
		this(currentPage, pagesOffset, bypass, DefaultStyle.INSTANCE);
	}

	public DefaultPageHandler(int currentPage, int pagesOffset, String bypass, IHtmlStyle style)
	{
		_currentPage = currentPage;
		_pagesOffset = pagesOffset;
		_bypass = bypass;
		_style = style;
	}
	
	@Override
	public void apply(int pages, StringBuilder sb)
	{
		final int pagerStart = Math.max(_currentPage - _pagesOffset, 0);
		final int pagerFinish = Math.min(_currentPage + _pagesOffset + 1, pages);
		
		// Show the initial pages in case we are in the middle or at the end
		if (pagerStart > _pagesOffset)
		{
			for (int i = 0; i < _pagesOffset; i++)
			{
				sb.append(_style.formatBypass(_bypass + " " + i, String.valueOf(i + 1), _currentPage == i));
			}
			
			// Separator
			sb.append(_style.formatSeparator());
		}

		// Show current pages
		for (int i = pagerStart; i < pagerFinish; i++)
		{
			sb.append(_style.formatBypass(_bypass + " " + i, String.valueOf(i + 1), _currentPage == i));
		}
		
		// Show the last pages
		if (pages > pagerFinish)
		{
			// Separator
			sb.append(_style.formatSeparator());

			for (int i = pages - _pagesOffset; i < pages; i++)
			{
				sb.append(_style.formatBypass(_bypass + " " + i, String.valueOf(i + 1), _currentPage == i));
			}
		}
	}
}