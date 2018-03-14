import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';
import ProjectEntryFiles from './ProjectEntryFiles.jsx';
import ProjectTypeView from './EntryViews/ProjectTypeView.jsx';
import ProjectEntryFilterComponent from './filter/ProjectEntryFilterComponent.jsx';
import WorkingGroupEntryView from './EntryViews/WorkingGroupEntryView.jsx';
import CommissionEntryView from './EntryViews/CommissionEntryView.jsx';

class ProjectEntryList extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/api/project';
        this.filterEndpoint = '/api/project/list';
        
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Title","title"),
            {
                header: "Pluto project",
                key: "vidispineId",
                render: vsid=>vsid? <a target="_blank" href={this.props.plutoBaseUrl + "/" + vsid}>{vsid}</a> : <span className="value-not-present">(not set)</span>,
                headerProps: { className: 'dashboardheader'}
            },
            {
                header: "Project type",
                key: "projectTypeId",
                render: (typeId)=><ProjectTypeView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            GeneralListComponent.dateTimeColumn("Created", "created"),
            GeneralListComponent.standardColumn("Owner","user"),
            {
                header: "Working group",
                key: "workingGroupId",
                render: typeId=><WorkingGroupEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            {
                header: "Commission",
                key: "commissionId",
                render: typeId=><CommissionEntryView entryId={typeId}/>,
                headerProps: {className: 'dashboardheader'}
            },
            this.actionIcons(),
            {
                header: "",
                key: "id",
                headerProps: {className: 'dashboardheader'},
                render: projid=><a target="_blank" href={"pluto:openproject:" + projid}>Open project</a>
            }
        ];
    }

    getFilterComponent(){
        return <ProjectEntryFilterComponent filterDidUpdate={this.filterDidUpdate}/>
    }

    newElementCallback(event) {
        this.props.history.push("/project/new");
    }
}

export default ProjectEntryList;