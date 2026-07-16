import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { GroupAssignments } from '../../components/group-assignments/group-assignments';

@Component({
  selector: 'app-assignments',
  imports: [
    CommonModule,
    CardModule,
    GroupAssignments
  ],
  templateUrl: './assignments.html',
  styleUrl: './assignments.scss'
})
export class Assignments {
}
