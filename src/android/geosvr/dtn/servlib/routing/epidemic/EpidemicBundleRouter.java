/*
 *	  This file is part of the Bytewalla Project
 *    More information can be found at "http://www.tslab.ssvl.kth.se/csd/projects/092106/".
 *    
 *    Copyright 2009 Telecommunication Systems Laboratory (TSLab), Royal Institute of Technology, Sweden.
 *    
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 */
package android.geosvr.dtn.servlib.routing.epidemic;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import android.geosvr.dtn.DTNManager;
import android.geosvr.dtn.DTNService;
import android.geosvr.dtn.applib.DTNAPIBinder;
import android.geosvr.dtn.applib.DTNAPICode.dtn_api_status_report_code;
import android.geosvr.dtn.applib.DTNAPICode.dtn_bundle_payload_location_t;
import android.geosvr.dtn.applib.types.DTNBundleID;
import android.geosvr.dtn.applib.types.DTNBundlePayload;
import android.geosvr.dtn.applib.types.DTNBundleSpec;
import android.geosvr.dtn.applib.types.DTNEndpointID;
import android.geosvr.dtn.applib.types.DTNHandle;
import android.geosvr.dtn.apps.DTNAPIFailException;
import android.geosvr.dtn.apps.DTNOpenFailException;
import android.geosvr.dtn.apps.DTNSend;
import android.geosvr.dtn.servlib.bundling.Bundle;
import android.geosvr.dtn.servlib.bundling.Bundle.priority_values_t;
import android.geosvr.dtn.servlib.bundling.BundleDaemon;
import android.geosvr.dtn.servlib.bundling.BundlePayload.location_t;
import android.geosvr.dtn.servlib.bundling.BundleProtocol;
import android.geosvr.dtn.servlib.bundling.SDNV;
import android.geosvr.dtn.servlib.bundling.event.BundleDeleteRequest;
import android.geosvr.dtn.servlib.bundling.event.BundleEvent;
import android.geosvr.dtn.servlib.bundling.event.BundleReceivedEvent;
import android.geosvr.dtn.servlib.bundling.event.ContactUpEvent;
import android.geosvr.dtn.servlib.bundling.event.event_source_t;
import android.geosvr.dtn.servlib.geohistorydtn.config.BundleConfig;
import android.geosvr.dtn.servlib.geohistorydtn.log.GeohistoryLog;
import android.geosvr.dtn.servlib.naming.EndpointID;
import android.geosvr.dtn.servlib.naming.EndpointIDPattern;
import android.geosvr.dtn.servlib.routing.RouteEntry;
import android.geosvr.dtn.servlib.routing.TableBasedRouter;
import android.geosvr.dtn.servlib.routing.epidemic.BaseTLV.TLVType;
import android.geosvr.dtn.servlib.routing.epidemic.EpidemicNeighbor.EpidemicNeighborRecvState;
import android.geosvr.dtn.servlib.routing.epidemic.EpidemicNeighbor.EpidemicNeighborSendState;
import android.geosvr.dtn.servlib.routing.epidemic.HelloTLV.HelloFunctionType;
import android.geosvr.dtn.systemlib.util.IByteBuffer;
import android.geosvr.dtn.systemlib.util.SerializableByteBuffer;
import android.util.Log;

/**
 * This is a non-abstract version of TableBasedRouter.
 * 
 * @author Mahesh Bogadi Shankar Prasad (mabsp@kth.se)
 */

public class EpidemicBundleRouter extends TableBasedRouter implements Runnable 
{
	public static enum epidemic_header_result {

		AckAll("AckAll", (byte) 0x01), Failure("Failure", (byte) 0x03), NoSuccessAck(
				"NoSuccessAck", (byte) 0x00), ReturnReceipt("ReturnReceipt",
				(byte) 0x04), Success("Success", (byte) 0x02);

		private static final Map<String, epidemic_header_result> caption_map = new HashMap<String, epidemic_header_result>();
		private static final Map<Byte, epidemic_header_result> lookup = new HashMap<Byte, epidemic_header_result>();

		static {
			for (epidemic_header_result s : EnumSet
					.allOf(epidemic_header_result.class)) {
				lookup.put(s.getCode(), s);
				caption_map.put(s.getCaption(), s);
			}

		}

		public static epidemic_header_result get(byte code) {
			return lookup.get(code);
		}

		private String caption_;

		private byte code_;

		private epidemic_header_result(String caption, byte code) {
			this.caption_ = caption;
			this.code_ = code;
		}

		public String getCaption() {
			return caption_;
		}

		public byte getCode() {
			return code_;
		}

	}

	private static EpidemicBundleRouter instance;

	/*
	 * NoSuccessAck: Result = 1 AckAll: Result = 2 Success: Result = 3 Failure:
	 * Result = 4 ReturnReceipt Result = 5
	 */

	private final static String TAG = "EpidemicBundleRouter";

	public static EpidemicBundleRouter getInstance() {
		return instance;
	}

	private static HashMap<String, EpidemicNeighbor> neighbors = new HashMap<String, EpidemicNeighbor>();

	private EpidemicRegistration registration = new EpidemicRegistration(this);

	/*
	 * 0 1 2 3 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Protocol |Version| Flags | Result | Code |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Receiver Instance | Sender Instance |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Transaction Identifier |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |S|
	 * SubMessage Number | Length (SDNV) |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ | | ~
	 * Message Body ~ | |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */

	public EpidemicBundleRouter() {
		super();
		instance = this;
		
		//启动监听客户端发送命令线程
		(new Thread(new waitOrder())).start();
	}

	@Override
	public EpidemicRegistration getEpidemicRegistration() {
		return registration;
	}

	public String localEid() {
		return BundleDaemon.getInstance().local_eid().str();
	}

	public byte SDNVByteDecode(IByteBuffer buf) {
		int[] val = new int[1];
		SDNV.decode(buf, 1, val);
		return (byte) val[0];
	}

	public short SDNVShortDecode(IByteBuffer buf) {
		int[] val = new int[1];
		SDNV.decode(buf, 2, val);
		return (short) val[0];
	}

	private byte[] adjustLenAndReturnArray(IByteBuffer buf) {
		int len = buf.position();
		buf.position(14);
		buf.putShort((short) len);
		byte array[] = new byte[len];

		for (int i = 0; i < len; i++)
			array[i] = buf.array()[i];
		return array;
	}

	private IByteBuffer createEpidemicBundle(EpidemicNeighbor pn) {
		IByteBuffer epidemicBuffer = new SerializableByteBuffer(1000);
		epidemicBuffer.rewind();
		// Protocol
		epidemicBuffer.put((byte) 140);
		// Version | Flags
		epidemicBuffer.put((byte) (0x01 << 4));
		// result
		epidemicBuffer.put(epidemic_header_result.AckAll.getCode());
		// code
		epidemicBuffer.put((byte) 0);

		// Receiver Instance
		epidemicBuffer.putShort(pn.remote_instance_);
		// Sender Instance
		epidemicBuffer.putShort(pn.local_instance_);
		// Transaction Identifier
		epidemicBuffer.putInt(pn.sendTransactionId);
		// submessage number
		epidemicBuffer.putShort((short) 0);

		// length
		epidemicBuffer.putShort((short) 0);
		return epidemicBuffer;
	}

	/**
	 * @param buf
	 * @param hello
	 */
	private String getString(IByteBuffer buf, int length) {
		byte[] b = new byte[length];
		buf.get(b);
		return new String(b);
	}

	public void deliver_bundle(Bundle bundle) {
		IByteBuffer buf = new SerializableByteBuffer(1000);

		// Epidemic Control bundle
		if (!bundle.payload().read_data(0, bundle.payload().length(), buf)) {
			Log.e(TAG, "Erruor reading epidemic bundle");
			return;
		}

		// Log.d(TAG, toString(buf.array()));
		Log.d(TAG, "Bundle with length " + bundle.payload().length());

		EpidemicBundle hdr = parseEpidemicBundle(buf);
		EpidemicNeighbor pn;

		String remote_eid = bundle.source().str().split("/epidemic")[0];
		if (remote_eid == null) {
			Log.e("TAG", "Bundle recv : remote_eid == null");
			return;
		}

		BundleDaemon
				.getInstance()
				.post_at_head(
						new BundleDeleteRequest(
								bundle,
								BundleProtocol.status_report_reason_t.REASON_NO_ADDTL_INFO));

		if ((pn = neighbors.get(remote_eid)) == null) {
			pn = new EpidemicNeighbor(remote_eid);
			neighbors.put(remote_eid, pn);
		}
		pn.recvTansactionId = hdr.trans_id;
		Log.d(TAG, hdr.toString());

		/* First byte is type */
		switch (hdr.type) {
		case HELLO:
			handleHello(buf, pn, hdr);
			break;
		case ERROR:
			handleError(buf, pn, hdr);
			break;
		case RIBDICTIONARY:
			handleRIBDictionary(pn, hdr);
			break;
		case RIBINFORMATIONBASE:
			handleRIBInformationBase(pn, hdr);
			break;
		case BUNDLEOFFER:
			handleBundleOffer(buf, pn, hdr);
			break;
		case BUNDLERESPONSE:
			handleBundleResponse(buf, pn, hdr);
			break;
		default:
			Log.e(TAG, String.format("Unknown Epidemic control(%x) from %s",
					hdr.type, pn.remote_eid()));
		}

		/* received both */
		if (hdr.type == TLVType.RIBDICTIONARY
				|| hdr.type == TLVType.RIBINFORMATIONBASE
				|| (hdr.type == TLVType.HELLO && hdr.hello.function == HelloFunctionType.ACK_SUP_RECALC)) {
			if (pn.rIBDictionary != null && pn.rIBInformation != null
					&& pn.acked) {
				updateNeighborP_(pn);
//				sendBundleOffer(pn);
				pn.setRecvState(EpidemicNeighborRecvState.BUNDLEOFFER_SENT);
				reroute_all_bundles();
			}
		}
	}

	private void handleError(IByteBuffer buf, EpidemicNeighbor controller,
			EpidemicBundle hdr) {
		Log.d(TAG, "Received ERROR from " + controller.remote_eid());
	}

	private void handleHello(IByteBuffer buf, EpidemicNeighbor pn,
			EpidemicBundle hdr) {
		Log.d(TAG, String.format("Received HELLO(%s) from %s ",
				hdr.hello.function.getCaption(), pn.remote_eid()));
		notify(String.format("Received HELLO(%s)", hdr.hello.function
				.getCaption()), String.format("From %s", pn.remote_eid()));

		switch (hdr.hello.function) {
		case SYN:
			sendHello(pn, HelloFunctionType.SYNACK_SUP_RECALC);
			pn.setRecvState(EpidemicNeighborRecvState.SYN_ACK_SENT);

			/* new database is arriving */
			pn.rIBDictionary = null;
			pn.rIBInformation = null;
			pn.acked = false;
			break;
		case SYNACK_EXE_RECALC:
		case SYNACK_SUP_RECALC:
			if (pn.getSendState() == EpidemicNeighborSendState.SYN_SENT) {
				pn.setSendState(EpidemicNeighborSendState.SYNACK_RCVD);
				sendNext(pn);
			}
			break;
		case ACK_EXE_RECALC:
		case ACK_SUP_RECALC:
			pn.acked = true;
			break;
		case RSTACK:
			break;
		}
	}

	private void handleRIBDictionary(EpidemicNeighbor pn, EpidemicBundle hdr) {
		Log.d(TAG, "Received RIBDictionary from " + pn.remote_eid());
		notify("Received RIBDictionary", String.format("From %s", pn
				.remote_eid()));
		pn.rIBDictionary = hdr.rIBDictionary;
	}

	private void handleRIBInformationBase(EpidemicNeighbor pn, EpidemicBundle hdr) {
		Log.d(TAG, "Received RIBInformationBase from " + pn.remote_eid());
		notify("Received RIBInformationBase", String.format("From %s", pn
				.remote_eid()));
		pn.rIBInformation = hdr.rIBInformationBase;
	}

	/**
	 * @param pn
	 */
	private void updateNeighborP_(EpidemicNeighbor pn) {
		Iterator<Short> sid = pn.rIBInformation.entries.keySet().iterator();
		EndpointIDPattern pneid = new EndpointIDPattern(pn.remote_eid() + "/*");

		while (sid.hasNext()) {
			Short ssid = sid.next();
			String neid = pn.rIBDictionary.entries.get(ssid).eid;
			if (neid.equals(localEid()))
				continue;

			if (neid.equals(pn.remote_eid())) {
				continue;
			}

			float P = pn.rIBInformation.entries.get(ssid).pValue;
			EpidemicNeighbor p = neighbors.get(neid);
			if (p == null) {
				p = new EpidemicNeighbor(neid);
			}

			/*
			 * pn.P_() is P_(A-B) P is P_(B-C)
			 */
			p.update_transitivity(pn.P_(), P);
			Log.i(TAG, "transitivity updated " +  p.P_() + " from " + p);
			
			/* my propbability is less than this */
			if (p.P_() <= P) {
				EndpointIDPattern nepnp = new EndpointIDPattern(neid + "/*");

				route_table_.add_entry(new RouteEntry(nepnp, pneid));
				Log.i(TAG, "Added route " + pn.remote_eid() + " - " + p.P_()
						+ " " + neid + " - " + P);
			} else {
				Log.i(TAG, "Skipped route " + pn.remote_eid() + " - " + p.P_()
						+ " " + neid + " - " + P);
			}
		}

		pn.rIBDictionary = null;
		pn.rIBInformation = null;
		pn.acked = false;
	}

	private void sendBundleOffer(EpidemicNeighbor pn) {
		IByteBuffer buf = createEpidemicBundle(pn);
		createBundleOfferTLV(buf, pn);
		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, String.format("send Bundle offer %s", pn.remote_eid()));
		notify(String.format("send Bundle offer"), String.format("To %s", pn
				.remote_eid()));
	}

	private void notify(String s1, String s2) {
		DTNManager.getInstance().notify_user(s1, s2);
	}

	private void handleBundleOffer(IByteBuffer buf, EpidemicNeighbor pn,
			EpidemicBundle hdr) {
		Log.d(TAG, "Received BundleOffer from " + pn.remote_eid());
		pn.bundleOffer = hdr.bundleOffer;
		sendBundleResponse(pn);
	}

	private void sendBundleResponse(EpidemicNeighbor pn) {
		if (pn.bundleOffer == null)
			return;

		IByteBuffer buf = createEpidemicBundle(pn);

		BundleResponseTLV.createTLV(buf, pn.bundleOffer.entries);

		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, String.format("send Bundle Response %s", pn.remote_eid()));
		notify(String.format("send Bundle Response"), String.format("To %s", pn
				.remote_eid()));
		pn.bundleOffer = null;
	}

	private EpidemicBundle parseEpidemicBundle(IByteBuffer buf) {
		EpidemicBundle bundle = new EpidemicBundle();
		buf.rewind();

		// Protocol
		bundle.protocol = buf.get();

		// Version | Flags
		bundle.versionFlags = buf.get();

		// result
		bundle.result = buf.get();

		// code
		bundle.code = buf.get();

		// Receiver Instance
		bundle.receiver = buf.getShort();

		// Sender Instance
		bundle.sender = buf.getShort();

		// Transaction Identifier
		bundle.trans_id = buf.getInt();

		// submessage number
		bundle.submessage = buf.getShort();

		// length
		bundle.length = buf.getShort();

		// type
		bundle.type = TLVType.get(buf.get());

		switch (bundle.type) {
		case HELLO:
			Hello hello = bundle.hello;
			hello.function = HelloFunctionType.get(buf.get());
			hello.length = SDNVShortDecode(buf);
			hello.timer = SDNVByteDecode(buf);
			hello.eid_length = SDNVByteDecode(buf);
			hello.eid = getString(buf, hello.eid_length);
			break;
		case RIBDICTIONARY:
			RIBDictionary d = bundle.rIBDictionary;
			d.flags = buf.get();
			d.length = SDNVShortDecode(buf);
			d.rIBDEntryCount = SDNVShortDecode(buf);
			d.reserved = buf.getShort();

			for (int i = 0; i++ < d.rIBDEntryCount;) {
				RIBDictionaryEntry e = new RIBDictionaryEntry();
				e.stringID = SDNVShortDecode(buf);
				e.length = SDNVByteDecode(buf);
				e.reserved = buf.get();
				e.eid = getString(buf, e.length);
				d.entries.put(e.stringID, e);
			}
			break;
		case RIBINFORMATIONBASE:
			RIBInformationBase d1 = bundle.rIBInformationBase;
			d1.flags = buf.get();
			d1.length = SDNVShortDecode(buf);
			d1.rIBStringCount = SDNVShortDecode(buf);
			d1.reserved = buf.getShort();

			for (int i = 0; i++ < d1.rIBStringCount;) {
				RIBInformationBaseEntry e = new RIBInformationBaseEntry();
				e.stringID = SDNVShortDecode(buf);
				e.pValue = EpidemicNeighbor.getPFloat(SDNVShortDecode(buf));
				e.flags = buf.get();
				d1.entries.put(e.stringID, e);
			}
			break;
		case BUNDLEOFFER:
			BundleOffer bo = bundle.bundleOffer;
			// flags
			bo.flags = buf.get();
			// length
			bo.length = SDNVShortDecode(buf);
			// offer count
			bo.offerCount = SDNVShortDecode(buf);

			// reserve
			bo.reserve = buf.getShort();

			for (int i = 0; i++ < bo.offerCount;) {
				BundleOfferEntry boe = new BundleOfferEntry();
				// ID
				boe.id = SDNVShortDecode(buf);

				// B_flags
				boe.flags = buf.get();
				boe.reserve = buf.get();

				// Creation Timestamp time
				boe.creationTime = SDNVShortDecode(buf);
				// Creation Timestamp sequence number
				boe.seqNo = SDNVShortDecode(buf);

				bo.entries.add(boe);
			}

			break;

		case BUNDLERESPONSE:
			BundleResponse br = bundle.bundleResponse;
			// flags
			br.flags = buf.get();
			// length
			br.length = SDNVShortDecode(buf);
			// offer count
			br.offerCount = SDNVShortDecode(buf);

			// reserve
			br.reserve = buf.getShort();

			for (int i = 0; i++ < br.offerCount;) {
				BundleResponseEntry bre = new BundleResponseEntry();
				// ID
				bre.id = SDNVShortDecode(buf);

				// B_flags
				bre.flags = buf.get();
				bre.reserve = buf.get();

				// Creation Timestamp time
				bre.creationTime = SDNVShortDecode(buf);
				// Creation Timestamp sequence number
				bre.seqNo = SDNVShortDecode(buf);
				br.entries.add(bre);
			}

			break;
		}

		return bundle;
	}

	/*
	 * private int SDNVIntDecode(IByteBuffer buf) { int[] val = new int[1];
	 * SDNV.decode(buf, 4, val); return (short) val[0]; }
	 */
	private void handleBundleResponse(IByteBuffer buf, EpidemicNeighbor pn,
			EpidemicBundle hdr) {
		Log.d(TAG, "Received Bundle Response from " + pn.remote_eid());
		reroute_all_bundles();
	}

	/* send the error */
	@SuppressWarnings("unused")
	private void sendError(EpidemicNeighbor pn) {
		IByteBuffer buf = createEpidemicBundle(pn);
		ErrorTLV.createTLV(buf, new byte[2]);
		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, "send Error " + pn.remote_eid());
	}

	/* send the hello */
	private void sendHello(EpidemicNeighbor pn, HelloFunctionType helloFun) {
		IByteBuffer buf = createEpidemicBundle(pn);
		HelloTLV.createTLV(buf, helloFun);

		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, String.format("send Hello(%s) %s", helloFun.getCaption(), pn
				.remote_eid()));
		notify(String.format("send Hello(%s)", helloFun.getCaption()), String
				.format("To %s", pn.remote_eid()));
	}

	private void sendMsg(final byte[] payload, EpidemicNeighbor pn) {
		Bundle bundle = new Bundle(location_t.MEMORY);
		bundle.set_dest(new EndpointID(pn.remote_eid() + "/epidemic"));
		bundle.set_source(new EndpointID(BundleDaemon.getInstance().local_eid()
				.str()
				+ "/epidemic"));
		bundle.set_prevhop(BundleDaemon.getInstance().local_eid());
		bundle.set_custodian(EndpointID.NULL_EID());
		bundle.set_replyto(new EndpointID(BundleDaemon.getInstance()
				.local_eid().str()
				+ "/epidemic"));
//		bundle.set_singleton_dest(true);
		bundle.set_singleton_dest(false);
		bundle.set_expiration(10000);
		bundle.set_priority(priority_values_t.COS_EXPEDITED);
		bundle.payload().set_data(payload);

		Log.d(TAG, toString(payload));
		// BundleDaemon.getInstance().post_at_head(new
		// BundleReceivedEvent(bundle, event_source_t.EVENTSRC_ADMIN));
		route_bundle(bundle);
		pn.timestamp = new Date().getTime();
	}

	private void sendNext(EpidemicNeighbor pn) {
		switch (pn.getSendState()) {
		case RIBIB_SENT:
			if (pn.timestamp + 5 * 60 * 1000 < new Date().getTime()) {
				pn.setSendState(EpidemicNeighborSendState.UNDEFINED);
				sendNext(pn);
			}
			break;
		case UNDEFINED:
			sendHello(pn, HelloFunctionType.SYN);
			pn.setSendState(EpidemicNeighborSendState.SYN_SENT);
			pn.update_encounter();
			break;
		case SYN_SENT:
			break;
		case SYNACK_RCVD:
			sendHello(pn, HelloFunctionType.ACK_SUP_RECALC);
			pn.setSendState(EpidemicNeighborSendState.SYN_ACK_ACK_SENT);
			sendNext(pn);
			break;
		case SYN_ACK_ACK_SENT:
			sendRIBDictionary(pn);
			pn.setSendState(EpidemicNeighborSendState.RIBDICTIONARY_SENT);
			sendNext(pn);
			break;
		case RIBDICTIONARY_SENT:
			sendRIBInformation(pn);
			pn.setSendState(EpidemicNeighborSendState.RIBIB_SENT);
			sendNext(pn);
			break;
		}
	}

	private void sendRIBDictionary(EpidemicNeighbor pn) {
		IByteBuffer buf = createEpidemicBundle(pn);
		RIBDictionaryTLV.createTLV(buf, neighbors);
		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, "Send RIBDictionary " + pn.remote_eid());
		notify("send RIBDictionary", String.format("To %s", pn.remote_eid()));
	}

	private void sendRIBInformation(EpidemicNeighbor pn) {
		RIBInformationBaseTLV ribInfo = new RIBInformationBaseTLV();
		IByteBuffer buf = createEpidemicBundle(pn);
		ribInfo.createTLV(buf, neighbors);
		sendMsg(adjustLenAndReturnArray(buf), pn);
		Log.d(TAG, "send RIBInformation " + pn.remote_eid());
		notify("send RIBInformationBase", String.format("To %s", pn
				.remote_eid()));
	}

	private String toString(byte[] br) {
		String st = "";
		for (byte b : br) {
			st += String.format("%x", b);
		}
		return st;
	}

	// protected void handle_bundle_transmitted(BundleTransmittedEvent event) {
	// String eid = event.bundle().dest().str();
	// super.handle_bundle_transmitted(event);
	//
	// if (eid.endsWith("/epidemic")) {
	// Log.i(TAG, "########Epidemic Bundle Transmitted");
	// EpidemicNeighbor pn = neighbors.get(eid.split("/epidemic")[0]);
	// sendNext(pn);
	// }
	// }

	/* Contact up */
	@Override
	protected void handle_contact_up(ContactUpEvent event) {
		super.handle_contact_up(event);

		EpidemicNeighbor pn = null;
		String remote_eid = event.contact().link().remote_eid().str();

		if (remote_eid.equals(localEid())) {
			Log.d(TAG, "Link ID is equal self");
			return;
		}
		// Add new contact.
		if ((pn = neighbors.get(remote_eid)) == null) {
			pn = new EpidemicNeighbor(remote_eid);
			neighbors.put(pn.remote_eid(), pn);
			Log.d(TAG, "New neighbor " + pn.remote_eid());
		}

		sendNext(pn);
	}

	// TLV Type
	// The TLV Type for a Bundle Offer is 0xA2. The TLV Type for a
	// Bundle Response is 0xA3.

	/*
	 * 0 1 2 3 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ | TLV
	 * Type | Flags | Length |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle Offer Count | Reserved |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle Dest String Id 1 (SDNV)| B_flags | resv |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle 1 Creation Timestamp time | | (variable length SDNV) |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle 1 Creation Timestamp sequence number | | (variable length SDNV) |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ ~ . ~ ~
	 * . ~ ~ . ~
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle Dest String Id n (SDNV)| B_flags | resv |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle n Creation Timestamp time | | (variable length SDNV) |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ |
	 * Bundle n Creation Timestamp sequence number | | (variable length SDNV) |
	 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
	 */

	public void createBundleOfferTLV(IByteBuffer buf, EpidemicNeighbor pn) {
		try {
			pending_bundles_.get_lock().lock();
			ListIterator<Bundle> bundles = pending_bundles_.begin();

			buf.put(BaseTLV.TLVType.BUNDLEOFFER.getCode());
			int start = buf.position();
			buf.put((byte) 0);
			// length
			SDNV.encode(4, buf, 2);
			// offer count
			SDNV.encode(0, buf, 2);
			// reserve
			buf.putShort((short) 0);

			int offerCount = 0;
			Log.d(TAG, "No of pending bundles " + pending_bundles_.size());
			while (bundles.hasNext()) {
				Bundle b = bundles.next();

				if (b.dest().str().endsWith("/epidemic")) {
					continue;
				}

				Log.d(TAG, b.bundleid() + " is offered to " + b.dest());
				// ID
				SDNV.encode(b.bundleid(), buf, 2);
				// B_flags
				buf.putShort((byte) 0);

				// Creation Timestamp time
				SDNV.encode(b.creation_ts().seconds(), buf, 2);
				// Creation Timestamp sequence number
				SDNV.encode(b.creation_ts().seqno(), buf, 2);
				offerCount++;
			}
			int end = buf.position();

			/* fill the header */
			buf.position(start);
			buf.put((byte) 0);
			// length
			SDNV.encode(end - start, buf, 2);
			// offer count
			SDNV.encode(offerCount, buf, 2);

			buf.position(end);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	//这里是对发送的处理，epidemic路由对发送的处理
	
	private BundleEvent event_;//传递给线程执行的event_
	
	@Override
	public void thread_handle_event(BundleEvent event) {
		// TODO Auto-generated method stub
		this.event_=event;
		(new Thread(this)).start();
	}

	@Override
	public void run()
	{
		super.handle_event(event_);
	}

	@Override
	protected void handle_bundle_received(BundleReceivedEvent event) {
		// TODO Auto-generated method stub
		event.bundle().setCancelOtherLinkTransmit(false);
		super.handle_bundle_received(event);
	}

	
	public class waitOrder implements Runnable{
		@Override
		public void run() {
			
			String tag="GeohistoryRouter.waitOrder";
			
			//数据
			int[] areaid_abnl={2138123745,2138123745,1587203,912940};
			int[] areaid_bcmn={1854974240,1854974240,1587203,912940};
			int[] areaid_nmij={-259923744,-259923744,1587203,912940};
			int[] areaid_lnjk={-479623740,-479623740,1587203,912940};
			
			int[] areaid_cdom={1645316860,1645316860,1587203,912940};
			int[] areaid_defo={1424349019,1424349019,1587203,912940};
			int[] areaid_ofgh={-1134001844,-1134001844,1587203,912940};
			int[] areaid_mohi={278199281,278199281,1587203,912940};
			
			HashMap<String,int[]> areaids=new HashMap<String, int[]>();
			areaids.put("abnl", areaid_abnl);
			areaids.put("bcmn", areaid_bcmn);
			areaids.put("nmij", areaid_nmij);
			areaids.put("lnjk", areaid_lnjk);
			
			areaids.put("cdom", areaid_cdom);
			areaids.put("defo", areaid_defo);
			areaids.put("ofgh", areaid_ofgh);
			areaids.put("mohi", areaid_mohi);
			
			
			String eid_14="dtn://192.168.5.14.wu.com";
			String eid_13="dtn://192.168.5.13.wu.com";
			String eid_12="dtn://192.168.5.12.wu.com";
			String eid_11="dtn://192.168.5.11.wu.com";
			
			File payload1=new File("/sdcard/dtnMessage/dtnMessage_1.txt");
			File payload2=new File("/sdcard/dtnMessage/dtnMessage_2.txt");
			File payload3=new File("/sdcard/dtnMessage/dtnMessage_3.txt");
			 
			int port=64431;//端口号
			DatagramSocket server;
			byte[] buffer=new byte[1024];
			try {
				Log.i(tag,"start GeohistoryRouter.waitOrder");
				server=new DatagramSocket(port);
				
				while(true){
					if(BundleDaemon.getInstance().shutting_down())
						break;
					
					String eid=null;
					int areaid[]=null;
					File payload=null;
					
					StringBuilder backinfo=new StringBuilder();
					try {
						Log.i(tag,"GeohistoryRouter.waitOrder wait for request");

						DatagramPacket packet=new DatagramPacket(buffer, buffer.length);
						server.receive(packet);
						byte[] temp_byte=packet.getData();
						int length=temp_byte[0];
						String str=new String(temp_byte,1,length-1);
						
						Log.v(tag, String.format("receive cmd %s",str));
						Log.v(tag,String.format("receive cmd(utf):%s",new String(temp_byte, 1, length-1, Charset.forName("utf-8"))));
						
						String s[]=str.split(" ");
						for(int i=0;i<s.length;i++){
							
							String temp[]=s[i].split(":");
							if(temp.length!=2){
								Log.e(tag,String.format("错误的参数：%s",s[i]));
								backinfo.append(String.format("错误的参数：%s",s[i]));
								break;
							}
							String name=temp[0];
							String value=temp[1];
							
							if(name.equals("eid")){
								if(value.equals("11")){
									eid=eid_11;
								}
								else if(value.equals("12")){
									eid=eid_12;
								}
								else if(value.equals("13")){
									eid=eid_13;
								}
								else if(value.equals("14")){
									eid=eid_14;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else if(name.equals("payload")){
								if(value.equals("1")){
									payload=payload1;
								}
								else if(value.equals("2")){
									payload=payload2;
								}
								else if(value.equals("3")){
									payload=payload3;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else if(name.equals("areaid")){
								if(value.equals("abnl")){
									areaid=areaid_abnl;
								}
								else if(value.equals("bcmn")){
									areaid=areaid_bcmn;
								}
								else if(value.equals("nmij")){
									areaid=areaid_nmij;
								}
								else if(value.equals("lnjk")){
									areaid=areaid_lnjk;
								}
								else if(value.equals("cdom")){
									areaid=areaid_cdom;
								}
								else if(value.equals("defo")){
									areaid=areaid_defo;
								}
								else if(value.equals("ofgh")){
									areaid=areaid_ofgh;
								}
								else if(value.equals("mohi")){
									areaid=areaid_mohi;
								}
								else{
									Log.e(tag,String.format("错误的参数：%s",s[i]));
									backinfo.append(String.format("错误的参数：%s",s[i]));
									break;
								}
							}
							else{
								Log.e(tag,String.format("错误的参数：%s",s[i]));
								backinfo.append(String.format("错误的参数：%s",s[i]));
								break;
							}
						}
						
						/*if(eid!=null && payload!=null && areaid!=null){
							SendBundleMsg send0=new SendBundleMsg(eid, payload, false, areaid, Bundle.DATA_BUNDLE);
							messagequeue.add(send0);
							backinfo.append(String.format("send bundle to %s ,with areaid(%d.%d.%d.%d) paylod(%s)",
									eid,areaid[0],areaid[1],areaid[2],areaid[3],payload.getPath()));
							
						}*/
						if(eid!=null && payload!=null && areaid!=null){
							try {
								sendMessage(eid, payload, false, areaid, Bundle.DATA_BUNDLE);
							} catch (DTNOpenFailException e) {
								Log.e(TAG, "DTNOpenFailException");
								e.printStackTrace();
							} catch (DTNAPIFailException e) {
								e.printStackTrace();
								Log.e(TAG, "DTNAPIFailException");
							}
						}
						Log.v("epidemic", String.format("backinfo: %s",backinfo.toString()));
						
						//将反馈消息送给客户端
						DatagramPacket back=new DatagramPacket(backinfo.toString().getBytes(), backinfo.toString().getBytes().length,
								packet.getAddress(), packet.getPort());
						server.send(back);
						
					} catch (IOException e) {
						e.printStackTrace();
						break;
					}
				}
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}//end the waitOrder thread class
	
	public boolean sendMessage(String dest_eid,File file,boolean rctp,int[] areaid,int bundleType) throws UnsupportedEncodingException, DTNOpenFailException, DTNAPIFailException
	{
		if(!DTNService.is_running())
			return false;
		
		//判断bundle的类型
		if(bundleType==Bundle.DATA_BUNDLE)
		{
			if(areaid==null || areaid.length!=4)
			{
				GeohistoryLog.e(TAG, "DTN应用发送bundle时，目的节点的各层次区域信息不明确");
				return false;
			}
		}
		else if(bundleType!=Bundle.NEI_AREA_BUNDLE)
		{
			GeohistoryLog.e(TAG, "DTN发送bundle时，event_source不明确，既不是应用的bundle也不是邻居的bundle");
			return false;
		}
		
		DTNAPIBinder dtn_api_binder_=DTNService.getDTNAPIBinder();
		
		double dest_longitude = -1.0;
		double dest_latitude = -1.0;
		
		DTNBundlePayload dtn_payload = new DTNBundlePayload(dtn_bundle_payload_location_t.DTN_PAYLOAD_FILE);
		
		if(file==null || !file.exists())
			return false;
		else
			dtn_payload.set_file(file);//用指定的文件进行发送
//			dtn_payload.set_file(new File("/sdcard/test_0.5M.mp3"));

		// Start the DTN Communication
		DTNHandle dtn_handle = new DTNHandle();
		dtn_api_status_report_code open_status = dtn_api_binder_.dtn_open(dtn_handle);
		if (open_status != dtn_api_status_report_code.DTN_SUCCESS) throw new DTNOpenFailException();
		try
		{
			DTNBundleSpec spec = new DTNBundleSpec();
			
			// set destination from the user input
			spec.set_dest(new DTNEndpointID(dest_eid));
			spec.setDestLongitude(dest_longitude);
			spec.setDestLatitude(dest_latitude);
			// set the source EID from the bundle Daemon
			spec.set_source(new DTNEndpointID(BundleDaemon.getInstance().local_eid().toString()));
				
			// Set expiration in seconds, default to 1 hour
			spec.set_expiration(DTNSend.EXPIRATION_TIME);
			// no option processing for now
			if(rctp)//rctp为true表示执行的是带回复的bundle
				spec.set_dopts(2);
			else
				spec.set_dopts(DTNSend.DELIVERY_OPTIONS);
			// Set prority
			spec.set_priority(DTNSend.PRIORITY);
			
			dtn_api_status_report_code api_send_result ;

//			api_send_result = dtn_api_binder_.dtn_multiple_send(dtn_handle, spec, dtn_payload, 1);
			
			int count=1;//bundle发送的副本数
			if (!dtn_api_binder_.is_handle_valid(dtn_handle))
				api_send_result=dtn_api_status_report_code.DTN_EHANDLE_INVALID;
			DTNBundleID[] dtn_bundle_id = new DTNBundleID[count];
			Bundle[] b = new Bundle[count];
			for (int i=0; i<count; i++) {
				dtn_bundle_id[i] = new DTNBundleID();
				b[i] = new Bundle(location_t.DISK);
				b[i] = dtn_api_binder_.dtn_send_multiple_final(dtn_handle, spec, dtn_payload, dtn_bundle_id[i], b[i]);
				
				//检测添加数据
				/* private long timestamp;
		    	private long invalidtime;//bundle的失效时间
		    	private int zeroArea;//0层区域，最底层区域，也是可达层区域
		    	private int firstArea;//1层区域，区域数字有小到大，依次范围扩大
		    	private int secondArea;//2层区域
		    	private int thirdArea;//3层区域
		    	
		    	private int deliverBundleNum;//传递阶段的bundle数量
		    	private int floodBundleNum;//洪泛扩散阶段bundle的数量
		    	private int isFlooding;//是否进入过了flood阶段
		    	
		    	int bundleType=DATA_BUNDLE;//判断bundle的类型，属于邻居间交换区域信息的bundle，或者是数据bundle
		*/        
				
//				b[i].setZeroArea(33);
//				b[i].setFirstArea(44);
//				b[i].setSecondArea(55);
//				b[i].setThirdArea(66);
//				b[i].setDeliverBundleNum(77);
//				b[i].setFloodBundleNum(88);
//				b[i].setIsFlooding(99);
				//设置目的节点区域信息
				if(bundleType==Bundle.DATA_BUNDLE)
				{
					b[i].setZeroArea(areaid[0]);
					b[i].setFirstArea(areaid[1]);
					b[i].setSecondArea(areaid[2]);
					b[i].setThirdArea(areaid[3]);
				}
				//设置bundle的副本数目
				b[i].setDeliverBundleNum(BundleConfig.DELIVERBUNDLENUM);
				b[i].setFloodBundleNum(BundleConfig.FLOODBUNDLENUM);
				b[i].setIsFlooding(0);
				//设置bundle的类型
				b[i].setBundleType(bundleType);
			}
			
			for (int i = 0; i < count; i++) {
				//向邻居发送区域交换信息的bundle
//				BundleDaemon.getInstance().post(
//						new BundleReceivedEvent(b[i], event_source_t.EVENTSRC_NEIGHBOUR));
				BundleDaemon.getInstance().post(
						new BundleReceivedEvent(b[i], event_source_t.EVENTSRC_APP));
			}
			api_send_result=dtn_api_status_report_code.DTN_SUCCESS;
			
			// If the API fail to execute throw the exception so user interface can catch and notify users
			if (api_send_result != dtn_api_status_report_code.DTN_SUCCESS) {
				throw new DTNAPIFailException();
			}
		
		}
		finally
		{
			dtn_api_binder_.dtn_close(dtn_handle);
			GeohistoryLog.i(TAG, "发送数据包成功");
		}
		
		return true;
	}
	
}
