import React from 'react';
import {shallow,mount} from 'enzyme';
import ErrorViewComponent from '../../../app/multistep/common/CommissionSelector.jsx';
import sinon from 'sinon';
import assert from 'assert';
import moxios from 'moxios';
import CommissionSelector from "../../../app/multistep/common/CommissionSelector";
// import {Readable} from "node/stream";

const stream = require("stream");

describe("CommissionSelector", ()=>{
    beforeEach(()=>fetch.resetMocks());
    //afterEach(()=>moxios.uninstall());

    it("should load data from the server on mount and present a list of options", (done)=>{
        const valueSetSpy = sinon.spy();

        //fetch.once(()=>new Promise((resolve, reject)=>resolve(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))));
        const fakeStream = new ReadableStream({
            start(controller) {
                return pump();
                function pump(){
                    controller.enqueue(JSON.stringify( {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"}));
                    controller.enqueue(JSON.stringify( {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"}));
                    controller.enqueue(JSON.stringify( {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}));
                }
            }
        });

        fetch.once(fakeStream);

        const rendered = mount(<CommissionSelector workingGroupId={4}
                                                     selectedCommissionId={2}
                                                     showStatus="In production"
                                                     valueWasSet={valueSetSpy}/>);

        expect(rendered.find('img').props().src).toEqual("/assets/images/uploading.svg");

        expect(fetch.mock.calls.length).toEqual(1);
        expect(fetch.mock.calls[0][0]).toEqual('/api/pluto/commission/liststreamed');

        writer.write(JSON.stringify({result:[
                {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"},
                {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"},
                {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}
            ],status:"ok"})).then(()=> {
            rendered.update();

            const selectorBox = rendered.find('#commission-selector');
            expect(selectorBox.children().length).toEqual(3);
            expect(selectorBox.childAt(0).props().value).toEqual(1);
            expect(selectorBox.childAt(0).text()).toEqual("Keith");
            expect(selectorBox.childAt(1).props().value).toEqual(2);
            expect(selectorBox.childAt(1).text()).toEqual("Jen");
            expect(selectorBox.childAt(2).props().value).toEqual(3);
            expect(selectorBox.childAt(2).text()).toEqual("Sarah");

            expect(selectorBox.props().defaultValue).toEqual(2);
            done();
        }).catch(err=>{
            console.error(err);
            done.fail(err);
        });

        // return moxios.wait(()=>{
        //     const req = moxios.requests.mostRecent();
        //     try{
        //         expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
        //         expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
        //     } catch(err) {
        //         done.fail(err);
        //     }
        //     req.respondWith({
        //         status: 200,
        //         response: {result:[
        //             {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"},
        //             {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"},
        //             {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}
        //         ],status:"ok"}
        //     }).then(()=>{
        //         rendered.update();
        //         const selectorBox = rendered.find('#commission-selector');
        //         expect(selectorBox.children().length).toEqual(3);
        //         expect(selectorBox.childAt(0).props().value).toEqual(1);
        //         expect(selectorBox.childAt(0).text()).toEqual("Keith");
        //         expect(selectorBox.childAt(1).props().value).toEqual(2);
        //         expect(selectorBox.childAt(1).text()).toEqual("Jen");
        //         expect(selectorBox.childAt(2).props().value).toEqual(3);
        //         expect(selectorBox.childAt(2).text()).toEqual("Sarah");
        //
        //         expect(selectorBox.props().defaultValue).toEqual(2);
        //         done();
        //     }).catch(err=>done.fail(err));
        //})
    });

    it("should present an error if download fails", (done)=>{
        const valueSetSpy = sinon.spy();
        const rendered = mount(<CommissionSelector workingGroupId={4}
                                                   selectedCommissionId={2}
                                                   showStatus="In production"
                                                   valueWasSet={valueSetSpy}/>);

        return moxios.wait(()=>{
            const req = moxios.requests.mostRecent();
            try{
                expect(rendered.instance().state.error).toBeFalsy();
                expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
                expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
            } catch(err) {
                done.fail(err);
            }
            req.respondWith({
                status: 500,
                response: {detail:"hanungah",status:"error"}
            }).then(()=>{
                expect(rendered.instance().state.error).toBeTruthy();
                done();
            }).catch(err=>done.fail(err));
        })
    });

    it("should notify the callback when the value changes", (done)=>{
        const valueSetSpy = sinon.spy();
        const rendered = mount(<CommissionSelector workingGroupId={4}
                                                   selectedCommissionId={2}
                                                   showStatus="In production"
                                                   valueWasSet={valueSetSpy}/>);

        expect(rendered.find('img').props().src).toEqual("/assets/images/uploading.svg");

        return moxios.wait(()=>{
            const req = moxios.requests.mostRecent();
            try{
                expect(req.config.url).toEqual("/api/pluto/commission/list?length=150");
                expect(req.config.data).toEqual(JSON.stringify({workingGroupId: 4, status:"In production", match:"W_EXACT"}))
            } catch(err) {
                done.fail(err);
            }
            req.respondWith({
                status: 200,
                response: {result:[
                    {id: 1, collectionId: 1234, siteId: "VX", title:"Keith"},
                    {id: 2, collectionId: 2345, siteId: "VX", title:"Jen"},
                    {id: 3, collectionId: 3456, siteId: "VX", title:"Sarah"}
                ],status:"ok"}
            }).then(()=>{
                rendered.update();
                const selectorBox = rendered.find('#commission-selector');
                expect(selectorBox.children().length).toEqual(3);
                selectorBox.simulate('change', {target: {value: 3}});
                assert(valueSetSpy.calledWith(3));
                done();
            }).catch(err=>done.fail(err));
        })
    })
});