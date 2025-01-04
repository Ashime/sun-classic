package com.valiantgaming.databaseserver.database.dao.character;

import org.hibernate.Session;

public class CharacterDAO
{
    private Session session;

    public CharacterDAO(Session session)
    {
        this.session = session;
    }

    public void deleteCharacter(String username, String deletionCode)
    {

    }
}