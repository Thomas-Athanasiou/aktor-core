package com.aktor.core.service;

import com.aktor.core.Service;

public interface IdentityService<Key>
extends Service
{
    Key generate();
}
