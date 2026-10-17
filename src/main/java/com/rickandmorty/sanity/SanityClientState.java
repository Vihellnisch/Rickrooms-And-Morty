package com.rickandmorty.sanity;

/**
 * The local player's sanity as last sent by the server. Plain Java on purpose, so the
 * network handler can set it without loading any client classes on a dedicated server.
 */
public final class SanityClientState {
    public static volatile float value = Sanity.MAX;

    private SanityClientState() {
    }
}
