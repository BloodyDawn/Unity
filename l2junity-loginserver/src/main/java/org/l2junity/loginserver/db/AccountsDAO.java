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
package org.l2junity.loginserver.db;

import java.io.Closeable;

import org.l2junity.loginserver.db.dto.Account;
import org.l2junity.loginserver.db.mapper.AccountMapper;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.BindBean;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;

/**
 * @author Nos
 */
@RegisterMapper(AccountMapper.class)
public interface AccountsDAO extends Closeable
{
	@SqlUpdate("INSERT INTO `accounts`(`name`, `password`) VALUES(:name, :password)")
	@GetGeneratedKeys
	public long insert(@Bind("name") String name, @Bind("password") String password);
	
	@SqlUpdate("UPDATE `accounts` SET `password` = :password WHERE `id` = :id")
	public int updatePassword(@Bind("id") long id, @Bind("password") String password);
	
	@SqlUpdate("UPDATE `accounts` SET `password` = :password WHERE `id` = :id")
	public int updatePassword(@BindBean Account account);
	
	@SqlUpdate("UPDATE `accounts` SET `last_server_id` = :lastServerId WHERE `id` = :id")
	public int updateLastServerId(@Bind("id") long id, @Bind("lastServerId") short lastServerId);
	
	@SqlUpdate("UPDATE `accounts` SET `last_server_id` = :lastServerId WHERE `id` = :id")
	public int updateLastServerId(@BindBean Account account);
	
	@SqlQuery("SELECT * FROM `accounts` WHERE `id` = :id")
	public Account findById(@Bind("id") long id);
	
	@SqlQuery("SELECT * FROM `accounts` WHERE `name` = :name")
	public Account findByName(@Bind("name") String name);
	
	@Override
	public void close();
}
