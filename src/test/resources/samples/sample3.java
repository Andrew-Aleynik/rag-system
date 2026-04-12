package com.andrewaleynik.ragsystem.factories;

public interface Factory<D, E> {
    D createDomain();
    E createEntity();
}
