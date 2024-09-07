package mock;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.ClientIdentity;
import org.hyperledger.fabric.contract.Context;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Map;

public class MockContext extends Context {

    private ClientIdentity clientIdentity;

    public MockContext(MockIdentity initialIdentity) {
        super(new MockChaincodeStub(initialIdentity));
        setClientIdentity(initialIdentity);
    }

    public void setClientIdentity(MockIdentity id) {
        try {
            ((MockChaincodeStub) stub).setCreator(id);
            this.clientIdentity = new ClientIdentity(getStub());
        } catch (CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTimestamp(Instant ts) {
        ((MockChaincodeStub) stub).setTimestamp(ts);
    }

    public void setTxId(String txId) {
        ((MockChaincodeStub) stub).setTxId(txId);
    }

    public void setTransientData(Object o) {
        ((MockChaincodeStub) stub).setTransientData(Map.of(
                "request", new Gson().toJson(o).getBytes(StandardCharsets.UTF_8)
        ));
    }

    @Override
    public MockChaincodeStub getStub() {
        return (MockChaincodeStub) stub;
    }

    @Override
    public ClientIdentity getClientIdentity() {
        return clientIdentity;
    }
}
