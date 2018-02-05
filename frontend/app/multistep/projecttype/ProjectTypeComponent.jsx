import CommonMultistepComponent from '../common/CommonMultistepComponent.jsx';

class ProjectTypeComponent extends CommonMultistepComponent {
    /* CommonMultistepComponent includes an implementation of ComponentDidUpdate which
    updates the parent with our state
     */
    constructor(props){
        super(props);

        this.state = {
            name: null,
            opensWith: null,
            version: null
        }
    }

    render() {
        return <div>
            <h3>Project Type</h3>
            <table>
                <tbody>
                <tr>
                    <td>Name of project type</td>
                    <td><input id="project_type_name" className="inputs" value={this.name} onChange={event=>this.setState({name: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Opens with which Mac app?</td>
                    <td><input id="opens_with" className="inputs" value={this.opensWith} onChange={event=>this.setState({opensWith: event.target.value})}/></td>
                </tr>
                <tr>
                    <td>Minimum required software version to open it</td>
                    <td><input id="version" className="inputs" value={this.version} onChange={event=>this.setState({version: event.target.value})}/></td>
                </tr>
                </tbody>
            </table>
        </div>
    }
}

export default ProjectTypeComponent;