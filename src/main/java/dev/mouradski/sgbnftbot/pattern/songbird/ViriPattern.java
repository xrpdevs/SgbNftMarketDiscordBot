package dev.mouradski.sgbnftbot.pattern.songbird;

import dev.mouradski.sgbnftbot.model.Marketplace;
import dev.mouradski.sgbnftbot.model.Network;
import dev.mouradski.sgbnftbot.model.TransactionType;
import dev.mouradski.sgbnftbot.pattern.TransactionPattern;
import dev.mouradski.sgbnftbot.service.EthHelper;
import org.springframework.stereotype.Component;
import org.web3j.abi.TypeDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.Log;

import org.web3j.protocol.core.methods.response.Transaction;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


@Component
public class ViriPattern extends TransactionPattern {

    @Override
    protected Network getNetwork() {
        return Network.SONGBIRD;
    }

    @Override
    protected TransactionType getTransactionType() {
        return TransactionType.BUY;
    }

    @Override
    protected String getTransactionFunction() {
        return "0x263c1c68";
        // submitNewBit(address ctrAddr, uint256 tokenid, uint256 bidAmount);
        //return "0x2d48ab58";
        // auctionWinner(uint256 auctionId , address nftContract , uint256 tokenId , address buyer , uint256 saleprice)
    } // submitNewBid(address nftContract, uint256 tokenId, uint256 bidAmount)

    @Override
    protected Marketplace getMarketplace() {
        return Marketplace.ViriMP;
    }

    private static final org.slf4j.Logger log
            = org.slf4j.LoggerFactory.getLogger(ViriPattern.class);
    @Override
    protected String extractNftContract(Transaction transaction) throws InvocationTargetException, NoSuchMethodException, IllegalAccessException, IOException {
        Log _log = getLogs(transaction.getHash(), getNetwork());

        String cAddr = decodeInput(_log.getData()).get(0).toString();

        return cAddr.replace("0x000000000000000000000000", "0x");
    }

    public List<Object> decodeInput(String data) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String inputData = data.replace("0x", "");
        //String method = inputData.substring(0,10);
        log.error("DATA >>>>>> " +data);
        String nftContract = inputData.substring(0,64);
        String _tokenId = inputData.substring(65, 128);
        String buyerAddr = inputData.substring(129,192);
        String value = inputData.substring(193);
        Method refMethod = TypeDecoder.class.getDeclaredMethod("decode",String.class,int.class,Class.class);
        refMethod.setAccessible(true);
        Address nftAddress = (Address)refMethod.invoke(null,nftContract,0,Address.class);
        Address buyAddress = (Address)refMethod.invoke(null,buyerAddr,0,Address.class);
        Uint256 tokenId = (Uint256) refMethod.invoke(null,_tokenId,0,Uint256.class);
        Uint256 amount = (Uint256) refMethod.invoke(null,value,0,Uint256.class);
        log.error("NFTAddress >>>>> " +nftAddress.toString());
        log.error("TokenID >>>>>>>> {} " , tokenId.getValue());
        log.error("BuyerAddr >>>>>> " +buyAddress.toString());
        log.error("SaleAmount >>>>> {} " , ethHelper.valueToDouble(amount.getValue()));
        List<Object> output = new ArrayList<>();
        output.add(nftAddress.toString());
        output.add(tokenId);
        output.add(buyAddress.toString());
        output.add(amount);
        return(output);


    }

    @Override
    protected String extractBuyer(Transaction transaction) {
        return transaction.getFrom();
    }

    @Override
    protected Long extractTokenId(Transaction transaction) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        log.error("{}, {},", transaction.getHash(), getNetwork());
        Log _log = getLogs(transaction.getHash(), getNetwork());

        Uint256 cTokId = (Uint256) decodeInput(_log.getData()).get(1);


        return Long.parseLong(String.valueOf(cTokId.getValue()));
    }

    public Log getLogs(String trxHash, Network network) throws IOException {
        EthGetTransactionReceipt transactionReceipt = getWeb3(network).ethGetTransactionReceipt(trxHash).send();
        List<Log> _log = transactionReceipt.getResult().getLogs();
        Log op = null;
        for (Log value : _log) {
            if (value.getTopics().get(0).startsWith("0x2d48ab58")) {
                op = value;
                break;
            }
        }
        return(op);
    }

    public Web3j getWeb3(Network network) {
        if (Network.FLARE.equals(network)) {
            return EthHelper.flareWeb3;
        } else {
            return EthHelper.songbirdWeb3;
        }
    }

    @Override
    protected String getMarketplaceListingUrl(Transaction transaction) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        return "https://nft.viri.uk/itemdetails/" + extractNftContract(transaction) + "/" + extractTokenId(transaction);
    }

    @Override
    protected Double extracePrice(Transaction transaction)  {
        return ethHelper.valueToDouble(transaction.getValue());
    }
}
