/*
 * Copyright (C) 2004-2015 L2J Unity
 * 
 * This file is part of L2J Unity.
 * 
 * L2J Unity is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * L2J Unity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2junity.gameserver.cache;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2junity.Config;
import org.l2junity.commons.util.file.filter.HTMLFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Layane
 */
public class HtmCache
{
	private static final Logger LOGGER = LoggerFactory.getLogger(HtmCache.class);
	
	private final HTMLFilter htmlFilter = new HTMLFilter();
	
	private final Map<String, String> _cache = Config.LAZY_CACHE ? new ConcurrentHashMap<>() : new HashMap<>();
	
	private int _loadedFiles;
	private long _bytesBuffLen;
	
	protected HtmCache()
	{
		reload();
	}
	
	public void reload()
	{
		reload(Config.DATAPACK_ROOT);
	}
	
	public void reload(File f)
	{
		if (!Config.LAZY_CACHE)
		{
			LOGGER.info("Html cache start...");
			parseDir(f);
			LOGGER.info("Cache[HTML]: {} megabytes on {} files loaded", String.format("%.3f", getMemoryUsage()), getLoadedFiles());
		}
		else
		{
			_cache.clear();
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			LOGGER.info("Cache[HTML]: Running lazy cache");
		}
	}
	
	public void reloadPath(File f)
	{
		parseDir(f);
		LOGGER.info("Cache[HTML]: Reloaded specified path.");
	}
	
	public double getMemoryUsage()
	{
		return ((float) _bytesBuffLen / 1048576);
	}
	
	public int getLoadedFiles()
	{
		return _loadedFiles;
	}
	
	private void parseDir(File dir)
	{
		final File[] files = dir.listFiles();
		for (File file : files)
		{
			if (!file.isDirectory())
			{
				loadFile(file);
			}
			else
			{
				parseDir(file);
			}
		}
	}
	
	public String loadFile(File file)
	{
		if (htmlFilter.accept(file))
		{
			try
			{
				byte[] bytes = Files.readAllBytes(file.toPath());
				String content = new String(bytes, "UTF-8");
				content = content.replaceAll("(?s)<!--.*?-->", ""); // Remove html comments
				
				String oldContent = _cache.put(file.toURI().getPath().substring(Config.DATAPACK_ROOT.toURI().getPath().length()), content);
				if (oldContent == null)
				{
					_bytesBuffLen += bytes.length;
					_loadedFiles++;
				}
				else
				{
					_bytesBuffLen = (_bytesBuffLen - oldContent.length()) + bytes.length;
				}
				return content;
			}
			catch (Exception e)
			{
				LOGGER.warn("Problem with htm file:", e);
			}
		}
		return null;
	}
	
	public String getHtmForce(String prefix, String path)
	{
		String content = getHtm(prefix, path);
		if (content == null)
		{
			content = "<html><body>My text is missing:<br>" + path + "</body></html>";
			LOGGER.warn("Cache[HTML]: Missing HTML page: {}", path);
		}
		return content;
	}
	
	public String getHtm(String prefix, String path)
	{
		String newPath = null;
		String content;
		if ((prefix != null) && !prefix.isEmpty())
		{
			newPath = prefix + path;
			content = getHtm(newPath);
			if (content != null)
			{
				return content;
			}
		}
		
		content = getHtm(path);
		if ((content != null) && (newPath != null))
		{
			_cache.put(newPath, content);
		}
		
		return content;
	}
	
	private String getHtm(String path)
	{
		if ((path == null) || path.isEmpty())
		{
			return ""; // avoid possible NPE
		}

		return _cache.computeIfAbsent(path, k -> Config.LAZY_CACHE ? loadFile(new File(Config.DATAPACK_ROOT, k)) : null);
	}
	
	public boolean contains(String path)
	{
		return _cache.containsKey(path);
	}
	
	/**
	 * @param path The path to the HTM
	 * @return {@code true} if the path targets a HTM or HTML file, {@code false} otherwise.
	 */
	public boolean isLoadable(String path)
	{
		return htmlFilter.accept(new File(Config.DATAPACK_ROOT, path));
	}
	
	public static HtmCache getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final HtmCache _instance = new HtmCache();
	}
}
