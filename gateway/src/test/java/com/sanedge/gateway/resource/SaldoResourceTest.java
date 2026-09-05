package com.sanedge.gateway.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sanedge.gateway.dto.SaldoDto;
import com.sanedge.gateway.service.SaldoService;

import io.smallrye.mutiny.Uni;

@ExtendWith(MockitoExtension.class)
class SaldoResourceTest {

    @Mock private SaldoService saldoService;
    private SaldoResource saldoResource;

    @BeforeEach
    void setUp() throws Exception {
        saldoResource = new SaldoResource();
        Field f = SaldoResource.class.getDeclaredField("saldoService");
        f.setAccessible(true);
        f.set(saldoResource, saldoService);
    }

    @Test
    void listSaldos_Success() {
        SaldoDto.FindAllSaldoResponse dto = new SaldoDto.FindAllSaldoResponse(List.of(), "success", "ok");
        lenient().when(saldoService.listSaldos(anyInt(), anyInt(), anyString())).thenReturn(Uni.createFrom().item(dto));
        SaldoDto.FindAllSaldoResponse result = saldoResource.listSaldos(1, 10, "").await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void getSaldo_Success() {
        SaldoDto.FindByIdSaldoResponse dto = new SaldoDto.FindByIdSaldoResponse(null, "success", "ok");
        lenient().when(saldoService.getSaldo(anyInt())).thenReturn(Uni.createFrom().item(dto));
        SaldoDto.FindByIdSaldoResponse result = saldoResource.getSaldo(1).await().indefinitely();
        assertThat(result.status()).isEqualTo("success");
    }

    @Test
    void createSaldo_Success() {
        SaldoDto.CreateSaldoResponse dto = new SaldoDto.CreateSaldoResponse(null, "success", "created");
        lenient().when(saldoService.createSaldo(any())).thenReturn(Uni.createFrom().item(dto));
        SaldoDto.CreateSaldoResponse result = saldoResource.createSaldo(new SaldoDto.CreateSaldoRequest("1234567890", 100000)).await().indefinitely();
        assertThat(result.message()).isEqualTo("created");
    }
}
