package com.symphonia.pairing.application.usecase;

import com.symphonia.pairing.application.dto.command.RecordPairingSelectionCommand;

public interface RecordPairingSelectionUseCase {
    void record(RecordPairingSelectionCommand command);
}
