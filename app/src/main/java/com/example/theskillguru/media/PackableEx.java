package com.example.theskillguru.media;

public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
