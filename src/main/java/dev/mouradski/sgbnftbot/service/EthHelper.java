package dev.mouradski.sgbnftbot.service;

import dev.mouradski.sgbnftbot.model.Network;
import io.reactivex.Flowable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetCode;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;

@Component
@Slf4j
public class EthHelper {

    public static Web3j songbirdWeb3;

    public static Web3j flareWeb3;

    public EthHelper(@Qualifier("songbirdWeb3") Web3j songbirdWeb3, @Qualifier("flareWeb3") Web3j flareWeb3) {
        EthHelper.songbirdWeb3 = songbirdWeb3;
        EthHelper.flareWeb3 = flareWeb3;
    }


    // Check if the address is a contract, so we don't waste time trying to fetch data from wrong chain
    public Boolean addressIsContract(String contract, Network network) throws IOException {
        EthGetCode ethGetCode = getWeb3(network).ethGetCode(contract, DefaultBlockParameterName.LATEST).send();
        return !Objects.equals(ethGetCode.getCode(), "0x");
    }

    public Log getLog(String trxHash, Network network) throws IOException {
        EthGetTransactionReceipt transactionReceipt = getWeb3(network).ethGetTransactionReceipt(trxHash).send();
        return transactionReceipt.getResult().getLogs().stream().filter(logl -> logl.getTopics().size() == 4).findFirst().orElse(null);
    }

    public Double valueToDouble(BigInteger value) {
        return value.divide(BigInteger.valueOf(1000000000000000L)).doubleValue() / 1000;
    }

    public Transaction getTransaction(String trxHash, Network network) throws IOException {
        return getWeb3(network).ethGetTransactionByHash(trxHash).send().getTransaction().get();
    }

    public Flowable<Transaction> getFlowable(Network network) {
        return getWeb3(network).transactionFlowable();
    }


    public Optional<String> getTokenUri(String contract, Long tokenId, Network network) {

        org.web3j.abi.datatypes.Function function = new org.web3j.abi.datatypes.Function(
                "tokenURI",
                Collections.singletonList(new Uint256(tokenId)),
                Collections.singletonList(new TypeReference<Utf8String>() {
                }));

        String encodedFunction = FunctionEncoder.encode(function);

        //log.error("EncodedFunction {}, function {}", encodedFunction, function);

        //o
        try {
            org.web3j.protocol.core.methods.response.EthCall response =
                    getWeb3(network).ethCall(org.web3j.protocol.core.methods.request.Transaction.createEthCallTransaction(null, contract, encodedFunction), DefaultBlockParameterName.LATEST)
                            .sendAsync().get();

            //log.error("{} {} Response {} {} {} ", network, tokenId, response.getValue(), response.getError(), response.getRevertReason());


            @SuppressWarnings("rawtypes")
            List<Type> someTypes = FunctionReturnDecoder.decode(
                    response.getValue(), function.getOutputParameters());

            //log.error("Sometypes {}", someTypes.toString());

            return Optional.of(someTypes.get(0).toString());
        } catch (Exception e) {
            //log.error("Error retrieving token URI, contract : {}, id : {}, network : {}", contract, tokenId, network);

            return Optional.empty();
        }
    }

    public Web3j getWeb3(Network network) {
        if (Network.FLARE.equals(network)) {
            return flareWeb3;
        } else {
            return songbirdWeb3;
        }
    }
}
