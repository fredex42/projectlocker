import React from 'react';
import GeneralListComponent from './GeneralListComponent.jsx';

class ProjectTemplateIndex extends GeneralListComponent {
    constructor(props){
        super(props);
        this.endpoint = '/template';
        this.columns = [
            {
                header: "Id",
                key: "id",
                defaultSorting: "desc",
                dataProps: { className: 'align-right'},
                headerProps: { className: 'dashboardheader'}
            },
            GeneralListComponent.standardColumn("Name","name"),
            GeneralListComponent.standardColumn("Project type", "projectType"),
            GeneralListComponent.standardColumn("Filepath", "filepath"),
            GeneralListComponent.standardColumn("Storage","storage")
        ];
    }
}

export default ProjectTemplateIndex;